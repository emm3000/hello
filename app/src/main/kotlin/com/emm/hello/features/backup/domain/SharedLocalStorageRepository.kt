package com.emm.hello.features.backup.domain

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.emm.hello.features.DataStore
import com.emm.hello.features.backup.DIRECTORY
import com.emm.hello.features.backup.DataModeler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class SharedLocalStorageRepository(
    private val dataModeler: DataModeler,
    private val context: Context,
) : LocalStorageRepository {

    private val dataStore: DataStore by lazy {
        DataStore(context)
    }

    override suspend fun save() = withContext(Dispatchers.IO) {
        val fileName = "random.json.gz"
        val jsonToSave: String = dataModeler.model()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentResolver: ContentResolver = context.contentResolver

            val existingUri: Uri? = try {
                Uri.parse(dataStore.uri)
            } catch (_: Exception) {
                findFileInDownloads()
            }

            val uri = existingUri ?: run {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/gzip")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY"))
                }
                contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            } ?: return@withContext

            contentResolver.openOutputStream(uri)?.gzipWrite(jsonToSave)

        } else {
            val state = Environment.getExternalStorageState()
            if (Environment.MEDIA_MOUNTED == state) {
                val directory = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY")
                )
                if (directory.exists().not()) {
                    directory.mkdirs()
                }
                File(directory, fileName).outputStream().gzipWrite(jsonToSave)
            }
            return@withContext
        }
    }

    private fun findFileInDownloads(): Uri? {
        val contentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.Downloads._ID)

        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("random.json.gz")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                    return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                }
            }
        } else {
            val state = Environment.getExternalStorageState()
            if (Environment.MEDIA_MOUNTED == state) {
                val directory = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY")
                )
                if (directory.exists().not()) {
                    directory.mkdirs()
                }
                val file = File(directory, "random.json.gz")
                if (file.exists()) {
                    return Uri.fromFile(file)
                }
            }
        }
        return null
    }

    override suspend fun read(uri: Uri) = withContext(Dispatchers.IO) {
        val jsonContent: String = context.contentResolver.openInputStream(uri)?.gzipRead() ?: "[]"
        dataModeler.inverse(jsonContent)
    }

    override suspend fun readPower() = withContext(Dispatchers.IO) {
        val findFileInDownloads: Uri = findFileInDownloads() ?: return@withContext
        val jsonContent: String = context.contentResolver.openInputStream(findFileInDownloads)?.gzipRead() ?: "[]"
        dataModeler.inverse(jsonContent)
    }

    private fun OutputStream.gzipWrite(data: String) {
        use { GZIPOutputStream(it).use { gzip -> gzip.write(data.toByteArray()) } }
    }

    private fun InputStream.gzipRead(): String = use {
        GZIPInputStream(it).bufferedReader().use(BufferedReader::readText)
    }
}
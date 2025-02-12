package com.emm.hello.features.backup.domain

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.emm.hello.features.backup.DIRECTORY
import com.emm.hello.features.backup.DataModeler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class SharedLocalStorageRepository(
    private val dataModeler: DataModeler,
    private val context: Context,
) : LocalStorageRepository {

    override suspend fun save() = withContext(Dispatchers.IO) {
        val fileName = fileName()
        val jsonToSave: String = dataModeler.model()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentResolver: ContentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/gzip")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY"))
            }
            val contentUri: Uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val uri: Uri = contentResolver.insert(contentUri, contentValues) ?: return@withContext

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

    private fun fileName(): String {
        val now: LocalDateTime = LocalDateTime.now()
        val ofPattern: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        val format: String = now.format(ofPattern)
        return "backup_$format.json.gzip"
    }

    override suspend fun read(uri: Uri) = withContext(Dispatchers.IO) {
        val jsonContent: String = context.contentResolver.openInputStream(uri)?.gzipRead() ?: "[]"
        dataModeler.inverse(jsonContent)
    }

    private fun OutputStream.gzipWrite(data: String) {
        use { GZIPOutputStream(it).use { gzip -> gzip.write(data.toByteArray()) } }
    }

    private fun InputStream.gzipRead(): String = use {
        GZIPInputStream(it).bufferedReader().use(BufferedReader::readText)
    }
}
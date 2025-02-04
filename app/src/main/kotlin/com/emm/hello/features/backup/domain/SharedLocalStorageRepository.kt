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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SharedLocalStorageRepository(
    private val dataModeler: DataModeler,
    private val context: Context,
) : LocalStorageRepository {

    override suspend fun save() = withContext(Dispatchers.IO) {
        val fileName = "hello-${currentLocalDateTime()}.json"
        val jsonToSave: String = dataModeler.model()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentResolver: ContentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY"))
            }
            val contentUri: Uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val uri: Uri = contentResolver.insert(contentUri, contentValues) ?: return@withContext

            contentResolver.openOutputStream(uri)?.use {
                it.write(jsonToSave.toByteArray())
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
                val file = File(directory, fileName)
                file.writeText(text = jsonToSave)
            }
            return@withContext
        }
    }

    private fun currentLocalDateTime(): String {
        val now: LocalDateTime = LocalDateTime.now()
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a")
        return now.format(formatter)
    }

    override suspend fun read(uri: Uri) = withContext(Dispatchers.IO) {
        val jsonContent: String = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bufferedReader: BufferedReader = inputStream.bufferedReader()
            val read: String = bufferedReader.use(BufferedReader::readText)
            return@use read
        } ?: "[]"
        dataModeler.inverse(jsonContent)
    }
}
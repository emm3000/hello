package com.emm.hello.features.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream

class JustFiles(
    private val dataModeler: DataModeler,
    private val context: Context,
) {

    suspend fun saveInternal() {
        val toSave: String = dataModeler.model()
        val createDirectory: File = context.createDirectory()
        val fileName = "first.json"
        val file = File(createDirectory, fileName)
        file.writeText(toSave)
    }

    fun deleteInternal() {
        val filePath = context.createDirectory()
        val file = File(filePath, "first.json")
        if (file.exists()) {
            file.delete()
        }
    }

    suspend fun saveExternal() {
        val toSave: String = dataModeler.model()
        val createDirectory: File = context.createPrivateExternalDir()
        val fileName = "second.json"
        val file = File(createDirectory, fileName)
        file.writeText(toSave)
    }

    fun deleteExternal() {
        val filePath = context.createPrivateExternalDir()
        val file = File(filePath, "second.json")
        if (file.exists()) {
            file.delete()
        }
    }

    suspend fun saveJsonToDownloads() {
        val toSave: String = dataModeler.model()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "third${System.currentTimeMillis()}.json")
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS.plus("/$DIRECTORY"))
            }

            val uri = resolver.insert(targetUri(), contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream: OutputStream ->
                    outputStream.write(toSave.toByteArray())
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "third.json")
            file.writeText(toSave)
        }
    }

    private fun targetUri(): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {

        MediaStore.Files.getContentUri("external")
    }
}
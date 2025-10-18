package com.emm.hello.legacy.backup

import android.content.Context
import java.io.File

const val DIRECTORY = "Bro"

fun Context.createDirectory(): File {
    val directory = File(this.filesDir, DIRECTORY)
    if (directory.exists().not()) {
        directory.mkdir()
    }
    return directory
}

fun Context.createPrivateExternalDir(): File {
    val extDirectory = getExternalFilesDir(null)
    val directory = File(extDirectory, DIRECTORY)
    if (directory.exists().not()) directory.mkdirs()
    return directory
}
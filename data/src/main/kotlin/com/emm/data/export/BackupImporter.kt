package com.emm.data.export

import android.net.Uri

interface BackupImporter {
    suspend fun import(inputUri: Uri): Result<Unit>
}

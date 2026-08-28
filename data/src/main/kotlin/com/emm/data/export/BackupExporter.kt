package com.emm.data.export

import android.net.Uri

interface BackupExporter {
    suspend fun export(outputUri: Uri): Result<Unit>
}

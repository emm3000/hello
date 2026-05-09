package com.emm.data.export

import android.net.Uri

/**
 * Abstraction for backup export operations.
 * Implemented by [ExportBackupDataSource].
 */
interface BackupExporter {
    suspend fun export(outputUri: Uri): Result<Unit>
}

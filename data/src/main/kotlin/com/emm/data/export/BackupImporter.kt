package com.emm.data.export

import android.net.Uri

/**
 * Abstraction for backup import operations.
 * Implemented by [ImportBackupDataSource].
 */
interface BackupImporter {
    suspend fun import(inputUri: Uri): Result<Unit>
}

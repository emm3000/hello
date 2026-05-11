package com.emm.hello.di

import com.emm.data.export.BackupExporter
import android.net.Uri

/**
 * No-op [BackupExporter] for instrumented tests.
 */
class FakeBackupExporter : BackupExporter {

    override suspend fun export(outputUri: Uri): Result<Unit> = Result.success(Unit)
}

/**
 * No-op [BackupImporter] for instrumented tests.
 */
class FakeBackupImporter : com.emm.data.export.BackupImporter {

    override suspend fun import(inputUri: Uri): Result<Unit> = Result.success(Unit)
}

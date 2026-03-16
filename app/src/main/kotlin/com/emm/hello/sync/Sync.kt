package com.emm.hello.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager

object Sync {

    fun initialize(context: Context) {
        WorkManager.getInstance(context).apply {
            enqueueUniquePeriodicWork(
                SYNC_WORK_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                SyncWorker.startUpSyncWorkPeriodic(),
            )
            enqueueUniqueWork(
                SYNC_WORK_IMMEDIATE_NAME,
                ExistingWorkPolicy.KEEP,
                SyncWorker.startUpSyncWorkOneShot(),
            )
        }
        ConnectivitySyncTrigger.register(context.applicationContext)
    }

    fun requestImmediate(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_WORK_IMMEDIATE_NAME,
            ExistingWorkPolicy.KEEP,
            SyncWorker.startUpSyncWorkOneShot(),
        )
    }
}

object BackupSync {

    fun backupInitialize(context: Context, force: Boolean = false) {
        WorkManager.getInstance(context).apply {
            enqueueUniqueWork(
                BACKUP_SYNC_WORK_NAME,
                androidx.work.ExistingWorkPolicy.KEEP,
                BackupSyncWorker.startUpSyncWork(force),
            )
        }
    }
}

internal const val SYNC_WORK_PERIODIC_NAME = "SyncWorkPeriodicName"
internal const val SYNC_WORK_IMMEDIATE_NAME = "SyncWorkImmediateName"
internal const val SYNC_WORK_TAG = "SyncWorkTag"
internal const val BACKUP_SYNC_WORK_NAME = "BACKUP_SYNC_WORK_NAME"

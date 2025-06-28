package com.emm.hello.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager

object Sync {

    fun initialize(context: Context) {

        WorkManager.getInstance(context).apply {

            enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                SyncWorker.startUpSyncWorkPeriodic(),
            )

//            enqueueUniqueWork(
//                SYNC_WORK_NAME,
//                ExistingWorkPolicy.KEEP,
//                SyncWorker.startUpSyncWork(),
//            )
        }
    }

    fun backupInitialize(context: Context) {
        WorkManager.getInstance(context).apply {
            enqueueUniqueWork(
                BACKUP_SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                BackupSyncWorker.startUpSyncWork(),
            )
        }
    }
}

internal const val SYNC_WORK_NAME = "SyncWorkName"
internal const val BACKUP_SYNC_WORK_NAME = "BACKUP_SYNC_WORK_NAME"
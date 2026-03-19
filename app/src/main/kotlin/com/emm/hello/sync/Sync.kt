package com.emm.hello.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager

object Sync {

    fun initialize(context: Context) {
        WorkManager.getInstance(context).apply {
            enqueueUniquePeriodicWork(
                SYNC_WORK_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                SyncEngineWorker.startUpSyncWorkPeriodic(),
            )
            enqueueUniqueWork(
                SYNC_WORK_IMMEDIATE_NAME,
                ExistingWorkPolicy.KEEP,
                SyncEngineWorker.startUpSyncWorkOneShot(),
            )
        }
        ConnectivitySyncTrigger.register(context.applicationContext)
    }

    fun requestImmediate(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_WORK_IMMEDIATE_NAME,
            ExistingWorkPolicy.KEEP,
            SyncEngineWorker.startUpSyncWorkOneShot(),
        )
    }
}

internal const val SYNC_WORK_PERIODIC_NAME = "SyncWorkPeriodicName"
internal const val SYNC_WORK_IMMEDIATE_NAME = "SyncWorkImmediateName"
internal const val SYNC_WORK_FOLLOW_UP_NAME = "SyncWorkFollowUpName"
internal const val SYNC_WORK_TAG = "SyncWorkTag"

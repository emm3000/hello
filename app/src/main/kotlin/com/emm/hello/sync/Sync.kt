package com.emm.hello.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.emm.data.sync.SyncRuntimePolicy

interface SyncWorkScheduler {
    fun initialize()
    fun requestImmediate()
    fun shutdown()
}

interface SyncGateway {
    fun initialize(context: Context, onConnectivityAvailable: () -> Unit)
    fun requestImmediate(context: Context)
    fun shutdown(context: Context)
}

class RuntimeAwareSyncWorkScheduler(
    private val appContext: Context,
    private val syncRuntimePolicy: SyncRuntimePolicy,
    private val syncGateway: SyncGateway = Sync,
) : SyncWorkScheduler {

    override fun initialize() {
        if (!syncRuntimePolicy.remoteEnabled) return
        syncGateway.initialize(appContext) { requestImmediate() }
    }

    override fun requestImmediate() {
        if (!syncRuntimePolicy.remoteEnabled) return
        syncGateway.requestImmediate(appContext)
    }

    override fun shutdown() {
        syncGateway.shutdown(appContext)
    }
}

object Sync : SyncGateway {

    override fun initialize(context: Context, onConnectivityAvailable: () -> Unit) {
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
        ConnectivitySyncTrigger.register(
            context = context.applicationContext,
            onNetworkAvailable = onConnectivityAvailable,
        )
    }

    override fun requestImmediate(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_WORK_IMMEDIATE_NAME,
            ExistingWorkPolicy.KEEP,
            SyncEngineWorker.startUpSyncWorkOneShot(),
        )
    }

    override fun shutdown(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(SYNC_WORK_PERIODIC_NAME)
            cancelUniqueWork(SYNC_WORK_IMMEDIATE_NAME)
            cancelUniqueWork(SYNC_WORK_FOLLOW_UP_NAME)
        }
        ConnectivitySyncTrigger.unregister(context.applicationContext)
    }
}

internal const val SYNC_WORK_PERIODIC_NAME = "SyncWorkPeriodicName"
internal const val SYNC_WORK_IMMEDIATE_NAME = "SyncWorkImmediateName"
internal const val SYNC_WORK_FOLLOW_UP_NAME = "SyncWorkFollowUpName"
internal const val SYNC_WORK_TAG = "SyncWorkTag"

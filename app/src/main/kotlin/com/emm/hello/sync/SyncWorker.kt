@file:Suppress("ConstPropertyName")

package com.emm.hello.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.emm.domain.sync.SyncEngine
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SyncWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val syncEngine: SyncEngine by inject<SyncEngine>()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            syncEngine.runOnce()
            Result.success()
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            if (e.isRecoverableSyncError()) Result.retry() else Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = appContext.syncForegroundInfo(
        channelName = SyncWorker,
        notificationId = SYNC_NOTIFICATION_ID,
        channelId = SYNC_NOTIFICATION_CHANNEL_ID,
    )

    companion object {

        const val SyncWorker: String = "SyncWorker"

        private const val SYNC_NOTIFICATION_ID = 0
        private const val SYNC_NOTIFICATION_CHANNEL_ID = "SyncNotificationChannel"

        fun startUpSyncWorkPeriodic(): PeriodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(SyncConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .addTag(SYNC_WORK_TAG)
            .build()

        fun startUpSyncWorkOneShot(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(SyncConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .addTag(SYNC_WORK_TAG)
            .build()
    }
}

private fun Throwable.isRecoverableSyncError(): Boolean {
    if (this is IOException) return true
    val raw = message?.lowercase().orEmpty()
    if (raw.isBlank()) return true
    return raw.contains("timeout") ||
        raw.contains("temporar") ||
        raw.contains("network") ||
        raw.contains("unavailable") ||
        raw.contains("connection") ||
        raw.contains("too many requests") ||
        raw.contains("429") ||
        raw.contains("5xx")
}

@file:Suppress("ConstPropertyName")

package com.emm.hello.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
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

class SyncEngineWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val syncEngine: SyncEngine by inject<SyncEngine>()

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            syncEngine.runOnce()
            Result.success()
        } catch (error: Exception) {
            FirebaseCrashlytics.getInstance().recordException(error)
            if (error.isRecoverableSyncError()) Result.retry() else Result.failure()
        }
    }

    companion object {

        private const val PERIODIC_INTERVAL_MINUTES = 15L
        private const val PERIODIC_BACKOFF_SECONDS = 30L
        private const val ONE_SHOT_BACKOFF_SECONDS = 10L

        fun startUpSyncWorkPeriodic(): PeriodicWorkRequest = PeriodicWorkRequestBuilder<SyncEngineWorker>(
            PERIODIC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(syncConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                PERIODIC_BACKOFF_SECONDS,
                TimeUnit.SECONDS,
            )
            .addTag(SYNC_WORK_TAG)
            .build()

        fun startUpSyncWorkOneShot(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncEngineWorker>()
            .setConstraints(syncConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                ONE_SHOT_BACKOFF_SECONDS,
                TimeUnit.SECONDS,
            )
            .addTag(SYNC_WORK_TAG)
            .build()
    }
}

private fun Throwable.isRecoverableSyncError(): Boolean {
    val raw = message?.lowercase().orEmpty()
    val isMessageRecoverable = raw.isBlank() ||
        raw.contains("timeout") ||
        raw.contains("temporar") ||
        raw.contains("network") ||
        raw.contains("unavailable") ||
        raw.contains("connection") ||
        raw.contains("too many requests") ||
        raw.contains("429") ||
        raw.contains("5xx")

    return this is IOException || isMessageRecoverable
}

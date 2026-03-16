@file:Suppress("ConstPropertyName")

package com.emm.hello.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.emm.domain.backup.RunBackupUseCase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class BackupSyncWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters), KoinComponent {

    val runBackupUseCase: RunBackupUseCase by inject<RunBackupUseCase>()

    private val force: Boolean = inputData.getBoolean("force", false)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runBackupUseCase(force)
            .fold(
                onSuccess = {
                    Result.success()
                },
                onFailure = {
                    FirebaseCrashlytics.getInstance().recordException(it)
                    if (it is SocketTimeoutException) {
                        return@withContext Result.retry()
                    }
                    Result.failure()
                }
            )
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = appContext.syncForegroundInfo(
        channelName = BackupSyncWorker,
        notificationId = SYNC_NOTIFICATION_ID,
        channelId = SYNC_NOTIFICATION_CHANNEL_ID,
    )

    companion object {

        const val BackupSyncWorker = "BackupSyncWorker"

        private const val SYNC_NOTIFICATION_ID = 1
        private const val SYNC_NOTIFICATION_CHANNEL_ID = "BackupSyncNotificationChannel"

        fun startUpSyncWork(force: Boolean): OneTimeWorkRequest = OneTimeWorkRequestBuilder<BackupSyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(SyncConstraints)
            .setInputData(
                Data.Builder()
                    .putBoolean("force", force)
                    .build()
            )
            .build()

        fun startUpSyncWorkPeriodic(): PeriodicWorkRequest = PeriodicWorkRequestBuilder<BackupSyncWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(SyncConstraints)
            .build()
    }
}

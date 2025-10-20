@file:Suppress("ConstPropertyName")

package com.emm.data.quote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.emm.data.flashcard.SyncConstraints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.IOException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class QuoteWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val quoteSynchronizer: QuoteSynchronizer by inject<QuoteSynchronizer>()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            quoteSynchronizer.execute()
            Result.success()
        } catch (t: IOException) {
            Result.retry()
        } catch (t: Exception) {
            Result.failure(
                workDataOf("aea" to t.message.toString())
            )
        }
    }

    companion object {

        const val QuoteWorkerName: String = "QuoteWorkerName"

        private fun startUpSyncWork(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<QuoteWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(SyncConstraints)
            .build()

        fun initialize(context: Context) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    QuoteWorkerName,
                    ExistingWorkPolicy.KEEP,
                    startUpSyncWork()
                )
        }
    }
}
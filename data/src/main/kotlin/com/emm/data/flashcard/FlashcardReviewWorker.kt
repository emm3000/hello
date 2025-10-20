@file:Suppress("ConstPropertyName")

package com.emm.data.flashcard

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FlashcardReviewWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val flashcardReviewSynchronizer: FlashcardReviewSynchronizer by inject<FlashcardReviewSynchronizer>()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            flashcardReviewSynchronizer.execute()
            Result.success()
        } catch (t: Throwable) {
            Result.failure(
                workDataOf("aea" to t.message.toString())
            )
        }
    }

    companion object {

        const val FlashcardReviewWorkerName: String = "FlashcardReviewWorkerName"

        private fun startUpSyncWork(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<FlashcardReviewWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(SyncConstraints)
            .build()

        fun initialize(context: Context) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    FlashcardReviewWorkerName,
                    ExistingWorkPolicy.KEEP,
                    startUpSyncWork()
                )
        }
    }
}
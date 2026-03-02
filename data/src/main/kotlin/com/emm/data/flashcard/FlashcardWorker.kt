@file:Suppress("ConstPropertyName")

package com.emm.data.flashcard

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
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

val SyncConstraints
    get() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

class FlashcardWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val flashcardSynchronizer: FlashcardSynchronizer by inject<FlashcardSynchronizer>()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            flashcardSynchronizer.execute()
            Result.success()
        } catch (t: Throwable) {
            Result.failure(
                workDataOf("aea" to t.message.toString())
            )
        }
    }

    companion object {

        const val FlashcardWorkerName: String = "FlashcardWorkerName"

        private fun startUpSyncWork(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<FlashcardWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(SyncConstraints)
            .build()

        fun initialize(context: Context) {
            WorkManager.getInstance(context)
                .beginUniqueWork(
                    FlashcardWorkerName,
                    ExistingWorkPolicy.KEEP,
                    startUpSyncWork()
                )
                .then(ExampleWorker.startUpSyncWork())
                .enqueue()
        }
    }
}

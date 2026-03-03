package com.emm.data.flashcard

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ExampleWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val exampleSynchronizer: ExampleSynchronizer by inject<ExampleSynchronizer>()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            exampleSynchronizer.execute()
            Result.success()
        } catch (t: Throwable) {
            Result.failure(
                workDataOf("aea" to t.message.toString())
            )
        }
    }

    companion object {

        fun startUpSyncWork(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<ExampleWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(SyncConstraints)
            .build()
    }
}

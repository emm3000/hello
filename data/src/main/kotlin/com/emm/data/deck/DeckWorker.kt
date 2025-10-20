@file:Suppress("ConstPropertyName")

package com.emm.data.deck

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
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

class DeckWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val deckSynchronizer: DeckSynchronizer by inject<DeckSynchronizer>()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            deckSynchronizer.execute()
            Result.success()
        } catch (t: Throwable) {
            Log.e("aea", t.message.toString())
            Result.failure(
                workDataOf("aea" to t.message.toString())
            )
        }
    }

    companion object {

        const val DeckWorkerName: String = "DeckWorkerName"

        private fun startUpSyncWork(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<DeckWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(SyncConstraints)
            .build()

        fun initialize(context: Context) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    DeckWorkerName,
                    androidx.work.ExistingWorkPolicy.KEEP,
                    startUpSyncWork()
                )
        }
    }
}
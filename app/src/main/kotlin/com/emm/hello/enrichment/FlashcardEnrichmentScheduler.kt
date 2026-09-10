package com.emm.hello.enrichment

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.emm.domain.ids.FlashcardId
import java.util.concurrent.TimeUnit

object FlashcardEnrichmentScheduler {

    private const val UNIQUE_WORK_PREFIX = "flashcard_enrichment_"
    private const val BACKOFF_DELAY_MINUTES = 5L

    fun enqueue(context: Context, flashcardId: FlashcardId) {
        val request = OneTimeWorkRequestBuilder<FlashcardEnrichmentWorker>()
            .setInputData(workDataOf(FlashcardEnrichmentWorker.KEY_FLASHCARD_ID to flashcardId.value))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_PREFIX + flashcardId.value,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}

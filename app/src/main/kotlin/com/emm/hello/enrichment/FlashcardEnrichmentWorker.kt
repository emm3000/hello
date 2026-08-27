package com.emm.hello.enrichment

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.emm.domain.authoring.EnrichCapturedFlashcardUseCase
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.ids.toFlashcardId
import org.koin.core.context.GlobalContext

class FlashcardEnrichmentWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val rawFlashcardId: String = inputData.getString(KEY_FLASHCARD_ID) ?: return Result.failure()
        val enrichCapturedFlashcard: EnrichCapturedFlashcardUseCase =
            GlobalContext.get().get<EnrichCapturedFlashcardUseCase>()

        val status: EnrichmentStatus = runCatching { enrichCapturedFlashcard(rawFlashcardId.toFlashcardId()) }
            .getOrElse { return retryOrFail() }

        return if (status == EnrichmentStatus.ENRICHED) Result.success() else retryOrFail()
    }

    private fun retryOrFail(): Result {
        return if (runAttemptCount + 1 >= MAX_ATTEMPTS) Result.failure() else Result.retry()
    }

    companion object {
        const val KEY_FLASHCARD_ID: String = "flashcardId"
        const val MAX_ATTEMPTS: Int = 3
    }
}

package com.emm.hello.enrichment

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.emm.domain.authoring.EnrichCapturedFlashcardUseCase
import com.emm.domain.authoring.MarkEnrichmentFailedUseCase
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.validation.DomainValidationException
import com.emm.hello.logging.logError
import kotlin.coroutines.cancellation.CancellationException
import org.koin.core.context.GlobalContext

class FlashcardEnrichmentWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val rawFlashcardId: String = inputData.getString(KEY_FLASHCARD_ID) ?: return Result.failure()
        val flashcardId: FlashcardId = rawFlashcardId.toFlashcardId()

        if (enrich(flashcardId) == EnrichmentStatus.ENRICHED) return Result.success()

        return retryOrGiveUp(flashcardId)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun enrich(flashcardId: FlashcardId): EnrichmentStatus {
        val enrichCapturedFlashcard: EnrichCapturedFlashcardUseCase =
            GlobalContext.get().get<EnrichCapturedFlashcardUseCase>()

        return try {
            enrichCapturedFlashcard(flashcardId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            logError(TAG, "enrich:error ${flashcardId.value} ${error.describe()}", error)
            EnrichmentStatus.FAILED
        }
    }

    private fun Throwable.describe(): String {
        if (this !is DomainValidationException) return message.orEmpty()
        return issues.joinToString(prefix = "domain_validation_failed [", postfix = "]") { issue ->
            "${issue.code.value}@${issue.field}"
        }
    }

    private suspend fun retryOrGiveUp(flashcardId: FlashcardId): Result {
        if (runAttemptCount + 1 < MAX_ATTEMPTS) return Result.retry()

        GlobalContext.get().get<MarkEnrichmentFailedUseCase>().invoke(flashcardId)
        logError(TAG, "enrich:abandoned ${flashcardId.value} after $MAX_ATTEMPTS attempts")
        return Result.failure()
    }

    companion object {
        const val KEY_FLASHCARD_ID: String = "flashcardId"
        const val MAX_ATTEMPTS: Int = 3
    }
}

private const val TAG = "FlashcardEnrichmentWorker"

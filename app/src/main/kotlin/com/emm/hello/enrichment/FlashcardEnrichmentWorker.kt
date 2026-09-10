package com.emm.hello.enrichment

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.emm.domain.authoring.EnrichCapturedFlashcardUseCase
import com.emm.domain.authoring.MarkEnrichmentFailedUseCase
import com.emm.domain.generation.EnrichmentFailure
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

        val error: Throwable = enrich(flashcardId) ?: return Result.success()

        if (!EnrichmentRetryPolicy.shouldRetry(error)) {
            markFailed(flashcardId, error)
            logError(TAG, "enrich:abandoned ${flashcardId.value} non_retryable")
            return Result.failure()
        }

        return retryOrGiveUp(flashcardId, error)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun enrich(flashcardId: FlashcardId): Throwable? {
        val enrichCapturedFlashcard: EnrichCapturedFlashcardUseCase =
            GlobalContext.get().get<EnrichCapturedFlashcardUseCase>()

        return try {
            enrichCapturedFlashcard(flashcardId)
            null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            logError(TAG, "enrich:error ${flashcardId.value} ${error.describe()}", error)
            error
        }
    }

    private fun Throwable.describe(): String {
        if (this !is DomainValidationException) return message.orEmpty()
        return issues.joinToString(prefix = "domain_validation_failed [", postfix = "]") { issue ->
            "${issue.code.value}@${issue.field}"
        }
    }

    private suspend fun retryOrGiveUp(flashcardId: FlashcardId, error: Throwable): Result {
        if (runAttemptCount + 1 < MAX_ATTEMPTS) return Result.retry()

        markFailed(flashcardId, error)
        logError(TAG, "enrich:abandoned ${flashcardId.value} after $MAX_ATTEMPTS attempts")
        return Result.failure()
    }

    private suspend fun markFailed(flashcardId: FlashcardId, error: Throwable) {
        val failure: EnrichmentFailure? = EnrichmentFailures.of(error)
        GlobalContext.get().get<MarkEnrichmentFailedUseCase>().invoke(flashcardId, failure)
    }

    companion object {
        const val KEY_FLASHCARD_ID: String = "flashcardId"
        const val MAX_ATTEMPTS: Int = 3
    }
}

private const val TAG = "FlashcardEnrichmentWorker"

package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.authoring.RetryEnrichmentUseCase
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.generation.EnrichmentFailureCause
import com.emm.domain.generation.GenerationCredits
import com.emm.domain.generation.GenerationCreditsRepository
import com.emm.domain.generation.canRetryAt
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.Clock
import com.emm.hello.R
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import com.emm.hello.newfeatures.shared.UndoEvent
import com.emm.hello.newfeatures.shared.UndoEventHolder
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class FlashcardDetailViewModel(
    private val flashcardId: String,
    private val flashcardRepository: FlashcardRepository,
    private val generationCredits: GenerationCreditsRepository,
    private val retryEnrichment: RetryEnrichmentUseCase,
    private val clock: Clock,
    private val undoEventHolder: UndoEventHolder,
) : MviViewModel<FlashcardDetailUiState, FlashcardDetailUiIntent, FlashcardDetailUiEffect>(
    initialState = FlashcardDetailUiState(),
) {

    private var observation: Job? = null

    override fun onIntent(intent: FlashcardDetailUiIntent) {
        when (intent) {
            FlashcardDetailUiIntent.Load -> observeFlashcard()
            FlashcardDetailUiIntent.BackClicked -> sendEffect(FlashcardDetailUiEffect.NavigateBack)
            FlashcardDetailUiIntent.EditFlashcard -> {
                sendEffect(FlashcardDetailUiEffect.NavigateToEditFlashcard(flashcardId))
            }
            FlashcardDetailUiIntent.DeleteFlashcard -> {
                setState { copy(isDeleteConfirmationVisible = true) }
            }
            FlashcardDetailUiIntent.ConfirmDeleteFlashcard -> deleteFlashcard()
            FlashcardDetailUiIntent.DismissDeleteFlashcard -> {
                setState { copy(isDeleteConfirmationVisible = false) }
            }
            FlashcardDetailUiIntent.TryAgainClicked -> retry()
        }
    }

    private fun observeFlashcard() {
        observation?.cancel()
        observation = combine(
            flashcardRepository.observeById(flashcardId.toFlashcardId()),
            generationCredits.observe(),
        ) { detail, credits -> show(detail, credits) }
            .catch { error ->
                logError(TAG, "observeFlashcard:error ${error.message}", error)
                sendEffect(FlashcardDetailUiEffect.LoadFailed(R.string.error_load_card))
            }
            .launchIn(viewModelScope)
    }

    private fun show(detail: FlashcardDetail?, credits: GenerationCredits?) {
        if (detail == null) {
            if (currentState.isLoading) sendEffect(FlashcardDetailUiEffect.LoadFailed(R.string.error_load_card))
            return
        }
        setState {
            copy(
                flashcard = detail.flashcard,
                isLoading = false,
                failedEnrichment = detail.flashcard.failedEnrichment(credits),
            )
        }
    }

    private fun Flashcard.failedEnrichment(credits: GenerationCredits?): FailedEnrichment? {
        val cause: EnrichmentFailureCause = enrichmentFailureCause ?: return null
        return FailedEnrichment(
            cause = cause,
            canRetry = cause.canRetryAt(clock.now(), credits),
            creditsResetAt = credits?.resetAt,
        )
    }

    private fun retry() = viewModelScope.launch {
        try {
            if (!retryEnrichment(flashcardId.toFlashcardId())) return@launch
            sendEffect(FlashcardDetailUiEffect.EnqueueEnrichment(flashcardId))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            logError(TAG, "retry:error ${error.message}", error)
            sendEffect(FlashcardDetailUiEffect.ShowMessage(R.string.card_detail_retry_error))
        }
    }

    private fun deleteFlashcard() = viewModelScope.launch {
        setState { copy(isDeleteConfirmationVisible = false) }
        try {
            val deletedAt: Long = flashcardRepository.softDeleteFlashcard(flashcardId.toFlashcardId())
            undoEventHolder.tryEmit(
                UndoEvent.CardDeleted(flashcardId = flashcardId, deletedAt = deletedAt)
            )
            sendEffect(FlashcardDetailUiEffect.FlashcardDeleted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "deleteFlashcard:error ${e.message}", e)
            sendEffect(FlashcardDetailUiEffect.ShowMessage(R.string.error_delete_card))
        }
    }
}

private const val TAG = "FlashcardDetailViewModel"

package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.SoftDeleteFlashcardUseCase
import com.emm.domain.ids.toFlashcardId
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import com.emm.hello.newfeatures.shared.UndoEvent
import com.emm.hello.newfeatures.shared.UndoEventHolder
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class FlashcardDetailViewModel(
    private val flashcardId: String,
    private val flashcardRepository: FlashcardRepository,
    private val softDeleteFlashcardUseCase: SoftDeleteFlashcardUseCase,
    private val undoEventHolder: UndoEventHolder,
) : MviViewModel<FlashcardDetailUiState, FlashcardDetailUiIntent, FlashcardDetailUiEffect>(
    initialState = FlashcardDetailUiState(),
) {

    init {
        onIntent(FlashcardDetailUiIntent.Load)
    }

    override fun onIntent(intent: FlashcardDetailUiIntent) {
        when (intent) {
            FlashcardDetailUiIntent.Load -> loadFlashcard()
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
        }
    }

    private fun loadFlashcard() {
        viewModelScope.launch {
            try {
                val detail = flashcardRepository.fetchById(flashcardId.toFlashcardId())
                setState { copy(flashcard = detail.flashcard, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logError(TAG, "loadFlashcard:error ${e.message}", e)
                sendEffect(FlashcardDetailUiEffect.LoadFailed("Couldn't load the card"))
            }
        }
    }

    private fun deleteFlashcard() = viewModelScope.launch {
        setState { copy(isDeleteConfirmationVisible = false) }
        try {
            val deletedAt = softDeleteFlashcardUseCase(flashcardId.toFlashcardId())
            undoEventHolder.tryEmit(
                UndoEvent.CardDeleted(flashcardId = flashcardId, deletedAt = deletedAt)
            )
            sendEffect(FlashcardDetailUiEffect.FlashcardDeleted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "deleteFlashcard:error ${e.message}", e)
            sendEffect(
                FlashcardDetailUiEffect.ShowMessage("Couldn't delete the card")
            )
        }
    }
}

private const val TAG = "FlashcardDetailViewModel"

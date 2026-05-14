package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.SoftDeleteFlashcardUseCase
import com.emm.domain.ids.toFlashcardId
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlinx.coroutines.launch

class FlashcardDetailViewModel(
    private val flashcardId: String,
    private val flashcardRepository: FlashcardRepository,
    private val softDeleteFlashcardUseCase: SoftDeleteFlashcardUseCase,
) : MviViewModel<FlashcardDetailUiState, FlashcardDetailUiIntent, FlashcardDetailUiEffect>(
    initialState = FlashcardDetailUiState(),
) {

    init {
        onIntent(FlashcardDetailUiIntent.Load)
    }

    override fun onIntent(intent: FlashcardDetailUiIntent) {
        when (intent) {
            FlashcardDetailUiIntent.Load -> loadFlashcard()
            FlashcardDetailUiIntent.EditFlashcard -> {
                sendEffect(FlashcardDetailUiEffect.NavigateToEditFlashcard(flashcardId))
            }
            FlashcardDetailUiIntent.DeleteFlashcard -> {
                setState { copy(showDeleteConfirmation = true) }
            }
            FlashcardDetailUiIntent.ConfirmDeleteFlashcard -> deleteFlashcard()
            FlashcardDetailUiIntent.DismissDeleteFlashcard -> {
                setState { copy(showDeleteConfirmation = false) }
            }
        }
    }

    private fun loadFlashcard() {
        viewModelScope.launch {
            runCatching { flashcardRepository.fetchById(flashcardId.toFlashcardId()) }
                .onSuccess { flashcard -> setState { copy(flashcard = flashcard) } }
                .onFailure {
                    sendEffect(FlashcardDetailUiEffect.LoadFailed(it.message ?: "load_failed"))
                }
        }
    }

    private fun deleteFlashcard() = viewModelScope.launch {
        setState { copy(showDeleteConfirmation = false) }
        runCatching {
            softDeleteFlashcardUseCase(flashcardId.toFlashcardId())
        }.onSuccess {
            sendEffect(FlashcardDetailUiEffect.FlashcardDeleted)
        }.onFailure { error ->
            logError(TAG, "deleteFlashcard:error ${error.message}", error)
            sendEffect(
                FlashcardDetailUiEffect.ShowMessage(error.message ?: "No se pudo eliminar la tarjeta")
            )
        }
    }
}

private const val TAG = "FlashcardDetailViewModel"

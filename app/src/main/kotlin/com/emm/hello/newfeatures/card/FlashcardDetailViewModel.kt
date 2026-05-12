package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.SoftDeleteFlashcardUseCase
import com.emm.domain.ids.toFlashcardId
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlinx.coroutines.flow.update
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
            FlashcardDetailUiIntent.EditFlashcard -> handleEditFlashcard()
            FlashcardDetailUiIntent.DeleteFlashcard -> {
                mutableState.update { it.copy(showDeleteConfirmation = true) }
            }
            FlashcardDetailUiIntent.ConfirmDeleteFlashcard -> deleteFlashcard()
            FlashcardDetailUiIntent.DismissDeleteFlashcard -> {
                mutableState.update { it.copy(showDeleteConfirmation = false) }
            }
        }
    }

    private fun loadFlashcard() {
        viewModelScope.launch {
            runCatching { flashcardRepository.fetchById(flashcardId.toFlashcardId()) }
                .onSuccess { flashcard -> mutableState.update { it.copy(flashcard = flashcard) } }
                .onFailure {
                    mutableEffect.send(FlashcardDetailUiEffect.LoadFailed(it.message ?: "load_failed"))
                }
        }
    }

    private fun handleEditFlashcard() = viewModelScope.launch {
        mutableEffect.send(FlashcardDetailUiEffect.NavigateToEditFlashcard(flashcardId))
    }

    private fun deleteFlashcard() = viewModelScope.launch {
        mutableState.update { it.copy(showDeleteConfirmation = false) }
        runCatching {
            softDeleteFlashcardUseCase(flashcardId.toFlashcardId())
        }.onSuccess {
            mutableEffect.send(FlashcardDetailUiEffect.FlashcardDeleted)
        }.onFailure { error ->
            logError(TAG, "deleteFlashcard:error ${error.message}", error)
            mutableEffect.send(
                FlashcardDetailUiEffect.ShowMessage(error.message ?: "No se pudo eliminar la tarjeta")
            )
        }
    }
}

private const val TAG = "FlashcardDetailViewModel"
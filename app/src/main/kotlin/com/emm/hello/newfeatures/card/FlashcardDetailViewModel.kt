package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.GetFlashcardByIdUseCase
import com.emm.hello.core.mvi.MviViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FlashcardDetailViewModel(
    private val flashcardId: String,
    private val getFlashcardByIdUseCase: GetFlashcardByIdUseCase,
) : MviViewModel<FlashcardDetailUiState, FlashcardDetailUiIntent, FlashcardDetailUiEffect>(
    initialState = FlashcardDetailUiState(),
) {

    init {
        onIntent(FlashcardDetailUiIntent.Load)
    }

    override fun onIntent(intent: FlashcardDetailUiIntent) {
        when (intent) {
            FlashcardDetailUiIntent.Load -> loadFlashcard()
        }
    }

    private fun loadFlashcard() {
        viewModelScope.launch {
            runCatching { getFlashcardByIdUseCase(flashcardId) }
                .onSuccess { flashcard -> mutableState.update { it.copy(flashcard = flashcard) } }
                .onFailure {
                    mutableEffect.send(FlashcardDetailUiEffect.LoadFailed(it.message ?: "load_failed"))
                }
        }
    }
}

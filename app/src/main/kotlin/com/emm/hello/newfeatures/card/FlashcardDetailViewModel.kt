package com.emm.hello.newfeatures.card

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.GetFlashcardByIdUseCase
import com.emm.hello.core.mvi.MviViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FlashcardDetailViewModel(
    private val flashcardId: String,
    private val getFlashcardByIdUseCase: GetFlashcardByIdUseCase,
) : MviViewModel<Flashcard, FlashcardDetailUiIntent, FlashcardDetailUiEffect>(
    initialState = Flashcard.Empty,
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
                .onSuccess { flashcard -> mutableState.update { flashcard } }
                .onFailure {
                    mutableEffect.send(FlashcardDetailUiEffect.LoadFailed(it.message ?: "load_failed"))
                }
        }
    }
}

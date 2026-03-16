package com.emm.hello.newfeatures.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.GetFlashcardByIdUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FlashcardDetailViewModel(
    private val flashcardId: String,
    private val getFlashcardByIdUseCase: GetFlashcardByIdUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(Flashcard.Empty)
    val uiState: StateFlow<Flashcard> = _state.asStateFlow()
    private val effectChannel = Channel<FlashcardDetailUiEffect>(Channel.BUFFERED)
    val effect: Flow<FlashcardDetailUiEffect> = effectChannel.receiveAsFlow()

    init {
        onIntent(FlashcardDetailUiIntent.Load)
    }

    fun onIntent(intent: FlashcardDetailUiIntent) {
        when (intent) {
            FlashcardDetailUiIntent.Load -> loadFlashcard()
        }
    }

    private fun loadFlashcard() {
        viewModelScope.launch {
            runCatching { getFlashcardByIdUseCase(flashcardId) }
                .onSuccess { flashcard -> _state.update { flashcard } }
                .onFailure {
                    effectChannel.send(
                        FlashcardDetailUiEffect.LoadFailed(it.message ?: "load_failed")
                    )
                }
        }
    }
}

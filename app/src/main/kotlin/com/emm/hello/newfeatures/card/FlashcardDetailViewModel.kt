package com.emm.hello.newfeatures.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.GetFlashcardByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FlashcardDetailViewModel(
    private val flashcardId: String,
    private val getFlashcardByIdUseCase: GetFlashcardByIdUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(Flashcard.Empty)
    val state: StateFlow<Flashcard> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val flashcard = getFlashcardByIdUseCase.find(flashcardId)
            _state.update { flashcard }
        }
    }
}

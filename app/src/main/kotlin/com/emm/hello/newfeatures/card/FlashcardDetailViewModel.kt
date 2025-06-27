package com.emm.hello.newfeatures.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardFinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FlashcardDetailViewModel(
    private val flashcardId: String,
    private val flashcardFinder: FlashcardFinder,
) : ViewModel() {

    private val _state = MutableStateFlow(Flashcard.Empty)
    val state: StateFlow<Flashcard> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val flashcard = flashcardFinder.find(flashcardId)
            _state.update { flashcard }
        }
    }
}

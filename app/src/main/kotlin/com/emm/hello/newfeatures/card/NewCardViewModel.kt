package com.emm.hello.newfeatures.card

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.data.remote.DataStore
import com.emm.domain.deck.DeckFetcher
import com.emm.domain.flashcard.FlashcardCreator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class NewCardViewModel(
    deckFetcher: DeckFetcher,
    private val cardCreator: FlashcardCreator,
    private val dataStore: DataStore,
) : ViewModel() {

    var state by mutableStateOf(NewCardUiState())
        private set

    init {
        deckFetcher.fetch()
            .onEach { decks ->
                val defaultDeckId = dataStore.defaultDeck
                val selectedDeck = decks.find { it.id == defaultDeckId } ?: decks.firstOrNull()
                state = state.copy(
                    decks = decks,
                    deckSelected = selectedDeck,
                    isCheck = defaultDeckId.isNotEmpty() && selectedDeck?.id == defaultDeckId
                )
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: NewCardAction) {
        when (action) {
            is NewCardAction.DeckSelected -> state = state.copy(
                deckSelected = action.deck,
                isCheck = dataStore.defaultDeck == action.deck.id,
            )
            is NewCardAction.WordChanged -> state = state.copy(word = action.word, error = null, previewResult = null)
            is NewCardAction.CheckChanged -> {
                val newDeckId = if (action.checked) state.deckSelected?.id.orEmpty() else ""
                dataStore.defaultDeck = newDeckId
                state = state.copy(isCheck = action.checked)
            }
            NewCardAction.GenerateClicked -> generateFlashcard()
            NewCardAction.SaveClicked -> saveFlashcard()
            is NewCardAction.CategorySelected -> state = state.copy(category = action.category, error = null, previewResult = null)
            is NewCardAction.DifficultySelected -> state = state.copy(difficulty = action.difficulty, error = null, previewResult = null)
            is NewCardAction.TypeViewSelected -> state = state.copy(typeView = action.typeView, previewResult = null)
            NewCardAction.SuccessConsumed -> state = state.copy(isSuccess = false)
        }
    }

    private fun generateFlashcard() = viewModelScope.launch {
        state = state.copy(isLoading = true, error = null, isSuccess = false)
        try {
            val preview = cardCreator.generateFlashcardPreview(
                word = state.word,
                categories = state.category,
                difficulty = state.difficulty,
                typeView = state.typeView,
            )
            state = state.copy(previewResult = preview, isLoading = false)
        } catch (e: Exception) {
            state = state.copy(error = e.message, isLoading = false)
        }
    }

    private fun saveFlashcard() = viewModelScope.launch {
        val deckId = state.deckSelected?.id ?: return@launch
        val preview = state.previewResult ?: return@launch
        state = state.copy(isLoading = true, error = null)
        try {
            cardCreator.saveFlashcard(
                deckId = deckId,
                flashcard = preview,
            )
            state = state.copy(
                word = "", 
                previewResult = null, 
                isLoading = false, 
                isSuccess = true
            )
        } catch (e: Exception) {
            state = state.copy(error = e.message, isLoading = false)
        }
    }
}

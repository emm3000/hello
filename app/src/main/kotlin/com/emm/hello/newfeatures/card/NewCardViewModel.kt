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
                state = state.copy(
                    decks = decks,
                    deckSelected = decks.firstOrNull(),
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
            is NewCardAction.WordChanged -> state = state.copy(word = action.word)
            is NewCardAction.CheckChanged -> {
                val newDeckId = if (action.checked) state.deckSelected?.id.orEmpty() else ""
                dataStore.defaultDeck = newDeckId
                state = state.copy(isCheck = action.checked)
            }
            NewCardAction.GenerateClicked,
            NewCardAction.SaveClicked -> createFlashcard()
            is NewCardAction.CategorySelected -> state = state.copy(category = action.category)
            is NewCardAction.DifficultySelected -> state = state.copy(difficulty = action.difficulty)
            is NewCardAction.TypeViewSelected -> state = state.copy(typeView = action.typeView)
        }
    }

    private fun createFlashcard() = viewModelScope.launch {
        val deckId = state.deckSelected?.id ?: return@launch
        state = state.copy(isLoading = true, error = null)
        try {
            val flashcard = cardCreator.createFlashcard(
                word = state.word,
                deckId = deckId,
                categories = state.category,
                difficulty = state.difficulty,
                typeView = state.typeView,
            )
            state = state.copy(word = "", result = flashcard, isLoading = false)
        } catch (e: Exception) {
            state = state.copy(error = e.message, isLoading = false)
        }
    }
}
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

    fun onAction(action: NewCardAction) {
        when (action) {
            is NewCardAction.OnDeckSelected -> state = state.copy(
                deckSelected = action.deck,
                isCheck = dataStore.defaultDeck == action.deck.id,
            )
            is NewCardAction.OnWordChanged -> state = state.copy(word = action.word)
            is NewCardAction.OnCheckChanged -> {
                if (action.checked) {
                    dataStore.defaultDeck = state.deckSelected?.id.orEmpty()
                    state = state.copy(isCheck = true)
                } else {
                    dataStore.defaultDeck = ""
                    state = state.copy(isCheck = false)
                }
            }
            NewCardAction.OnSaveClicked -> createFlashcard()
            NewCardAction.OnGenerateClicked -> createFlashcard()
            is NewCardAction.OnCategorySelected -> state = state.copy(category = action.category)
            is NewCardAction.OnDifficultySelected -> state = state.copy(difficulty = action.difficulty)
            is NewCardAction.OnTypeViewSelected -> state = state.copy(typeView = action.typeView)
        }
    }

    init {
        deckFetcher.fetch()
            .onEach { state = state.copy(decks = it, deckSelected = it.firstOrNull()) }
            .launchIn(viewModelScope)
    }

    private fun createFlashcard() = viewModelScope.launch {
        tryCreateFlashcard()
    }

    private suspend fun tryCreateFlashcard() {
        try {
            state = state.copy(isLoading = true)
            val deckId: String = state.deckSelected?.id ?: return
            val createFlashcard = cardCreator.createFlashcard(
                word = state.word,
                deckId = deckId,
                categories = state.category,
                difficulty = state.difficulty,
                typeView = state.typeView,
            )
            state = state.copy(word = "", result = createFlashcard, isLoading = false)
        } catch (e: Exception) {
            state = state.copy(error = e.message, isLoading = false)
        }
    }
}
package com.emm.hello.newfeatures.card

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.data.remote.DataStore
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckFetcher
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardCreator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class NewCardUiState(
    val word: String = "",
    val decks: List<Deck> = emptyList(),
    val deckSelected: Deck? = null,
    val isLoading: Boolean = false,
    val isCheck: Boolean = false,
    val error: String? = null,
    val result: Flashcard? = null,
)

sealed interface NewCardAction {

    data class OnWordChanged(val word: String) : NewCardAction

    data class OnDeckSelected(val deck: Deck) : NewCardAction

    data class OnCheckChanged(val checked: Boolean) : NewCardAction

    object OnGenerateClicked : NewCardAction

    object OnSaveClicked : NewCardAction
}

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
            )
            state = state.copy(word = "", result = createFlashcard, isLoading = false)
        } catch (e: Exception) {
            state = state.copy(error = e.message, isLoading = false)
        }
    }
}
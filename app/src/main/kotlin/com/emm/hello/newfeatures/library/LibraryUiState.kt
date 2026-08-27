package com.emm.hello.newfeatures.library

import com.emm.domain.deck.Deck
import com.emm.domain.ids.DeckId
import com.emm.domain.library.LibraryFlashcard
import com.emm.hello.core.mvi.MviState

data class LibraryUiState(
    val cards: List<LibraryFlashcard> = emptyList(),
    val decks: List<Deck> = emptyList(),
    val query: String = "",
    val selectedDeckId: DeckId? = null,
    val isLoading: Boolean = true,
) : MviState {

    val isFiltered: Boolean
        get() = query.isNotBlank() || selectedDeckId != null

    val isLibraryEmpty: Boolean
        get() = !isLoading && cards.isEmpty() && !isFiltered

    val hasNoResults: Boolean
        get() = !isLoading && cards.isEmpty() && isFiltered
}

data class LibrarySearchCriteria(
    val query: String = "",
    val deckId: DeckId? = null,
)

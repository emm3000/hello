package com.emm.hello.newfeatures.deck

import com.emm.domain.deck.Deck
import com.emm.hello.core.mvi.MviState

data class DecksUiState(
    val decks: List<Deck> = emptyList(),
    val isLoading: Boolean = true,
) : MviState {

    val isEmpty: Boolean
        get() = !isLoading && decks.isEmpty()
}

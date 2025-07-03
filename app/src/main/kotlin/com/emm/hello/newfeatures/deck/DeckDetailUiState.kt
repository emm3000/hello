package com.emm.hello.newfeatures.deck

import com.emm.domain.deck.Deck

data class DeckDetailUiState(
    val deck: Deck = Deck.Empty,
    val hasSessionEnabled: Boolean = false,
)

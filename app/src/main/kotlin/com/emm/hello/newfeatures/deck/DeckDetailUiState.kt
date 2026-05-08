package com.emm.hello.newfeatures.deck

import com.emm.domain.deck.Deck
import com.emm.domain.time.SystemClock

data class DeckDetailUiState(
    val deck: Deck = Deck.empty(SystemClock),
    val hasSessionEnabled: Boolean = false,
    val searchQuery: String = "",
)

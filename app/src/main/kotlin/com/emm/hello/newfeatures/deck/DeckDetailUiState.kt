package com.emm.hello.newfeatures.deck

import com.emm.domain.deck.Deck
import com.emm.domain.time.SystemClock
import com.emm.hello.core.mvi.MviState

data class DeckDetailUiState(
    val deck: Deck = Deck.empty(SystemClock),
    val hasSessionEnabled: Boolean = false,
    val searchQuery: String = "",
    val showDeleteConfirmation: Boolean = false,
) : MviState

package com.emm.hello.newfeatures.dashboard

import com.emm.domain.deck.Deck

data class DashboardUiState(
    val decks: List<Deck> = emptyList(),
)
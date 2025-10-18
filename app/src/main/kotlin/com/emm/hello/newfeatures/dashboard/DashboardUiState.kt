package com.emm.hello.newfeatures.dashboard

import com.emm.domain.deck.Deck
import com.emm.domain.quote.Quote

data class DashboardUiState(
    val decks: List<Deck> = emptyList(),
    val quote: Quote? = null,
    val isSyncing: Boolean = false,
    val lastUpdatedDate: String? = null,
)
package com.emm.hello.newfeatures.dashboard

import com.emm.domain.deck.Deck
import com.emm.domain.study.DashboardStats

data class DashboardUiState(
    val decks: List<Deck> = emptyList(),
    val isLoading: Boolean = true,
    val stats: DashboardStats? = null,
)

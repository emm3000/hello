package com.emm.hello.newfeatures.dashboard

import com.emm.domain.deck.Deck
import com.emm.domain.study.DashboardStats

data class DashboardUiState(
    val decks: List<Deck> = emptyList(),
    val totalDeckCount: Int = 0,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedTags: Set<String> = emptySet(),
    val availableTags: List<String> = emptyList(),
    val isFiltering: Boolean = false,
    val emptyState: DashboardEmptyState = DashboardEmptyState.None,
    val stats: DashboardStats? = null,
)

enum class DashboardEmptyState {
    None,
    LibraryEmpty,
    NoResults,
}

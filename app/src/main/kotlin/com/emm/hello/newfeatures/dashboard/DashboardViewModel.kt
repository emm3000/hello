package com.emm.hello.newfeatures.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckFetcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val decks: List<Deck> = emptyList(),
)

class DashboardViewModel(deckFetcher: DeckFetcher): ViewModel() {

    val state: StateFlow<DashboardUiState> = deckFetcher.fetch()
        .map(::DashboardUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState(),
        )
}
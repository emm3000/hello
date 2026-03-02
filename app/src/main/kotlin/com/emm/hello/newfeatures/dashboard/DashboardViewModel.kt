package com.emm.hello.newfeatures.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.DeckFetcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(deckFetcher: DeckFetcher) : ViewModel() {

    val state: StateFlow<DashboardUiState> = deckFetcher.fetch()
        .map { decks -> DashboardUiState(decks = decks, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState(isLoading = true),
        )
}

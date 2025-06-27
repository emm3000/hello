package com.emm.hello.newfeatures.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckFetcher
import com.emm.domain.quote.Quote
import com.emm.domain.quote.QuoteLastFetcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val decks: List<Deck> = emptyList(),
    val quote: Quote? = null
)

class DashboardViewModel(
    deckFetcher: DeckFetcher,
    quoteFetcher: QuoteLastFetcher
) : ViewModel() {

    val state: StateFlow<DashboardUiState> = combine(
        deckFetcher.fetch(),
        quoteFetcher.fetch()
    ) { decks: List<Deck>, quote: List<Quote> ->
        DashboardUiState(
            decks = decks,
            quote = quote.firstOrNull(),
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState(),
        )
}
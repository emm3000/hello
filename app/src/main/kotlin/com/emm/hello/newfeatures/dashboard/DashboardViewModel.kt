package com.emm.hello.newfeatures.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.data.remote.DataStore
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckFetcher
import com.emm.domain.quote.Quote
import com.emm.domain.quote.QuoteLastFetcher
import com.emm.hello.sync.WorkManagerSyncManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class DashboardViewModel(
    deckFetcher: DeckFetcher,
    quoteFetcher: QuoteLastFetcher,
    workManagerSyncManager: WorkManagerSyncManager,
    dataStore: DataStore,
) : ViewModel() {

    val state: StateFlow<DashboardUiState> = combine(
        deckFetcher.fetch(),
        quoteFetcher.fetch(),
        workManagerSyncManager.isSyncing,
        dataStore.lastUpdatedDate.map(::formatString),
    ) { decks: List<Deck>, quote: List<Quote>, isSyncing, lastUpdatedDate ->
        DashboardUiState(
            decks = decks,
            quote = quote.firstOrNull(),
            isSyncing = isSyncing,
            lastUpdatedDate = lastUpdatedDate,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState(),
        )

    private fun formatString(dateTimeString: String): String {
        val parse = LocalDateTime.parse(dateTimeString)
        val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault())
        return parse.format(formatter)
    }
}
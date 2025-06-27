package com.emm.hello.newfeatures.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DecksWithCardsProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DeckDetailViewModel(
    deckId: String,
    decksWithCardsProvider: DecksWithCardsProvider,
) : ViewModel() {

    val decks: StateFlow<Deck> = decksWithCardsProvider.provide(deckId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Deck.Empty,
        )
}
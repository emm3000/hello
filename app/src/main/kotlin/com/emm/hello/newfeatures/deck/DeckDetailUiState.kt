package com.emm.hello.newfeatures.deck

import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.Flashcard

data class DeckDetailUiState(
    val deck: Deck = Deck.Empty,
    val cardsSession: List<Flashcard> = emptyList(),
    val hasSessionEnabled: Boolean = false,
)

package com.emm.hello.newfeatures.deck

import com.emm.hello.core.mvi.MviIntent

sealed interface DecksUiIntent : MviIntent {
    data class DeckOpened(val deckId: String) : DecksUiIntent
    data object CreateDeckRequested : DecksUiIntent
    data class UndoDeleteDeck(val deckId: String, val deletedAt: Long) : DecksUiIntent
}

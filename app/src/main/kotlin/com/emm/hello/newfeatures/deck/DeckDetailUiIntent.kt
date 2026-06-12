package com.emm.hello.newfeatures.deck

import com.emm.hello.core.mvi.MviIntent

sealed interface DeckDetailUiIntent : MviIntent {
    data class SearchCardsChanged(val query: String) : DeckDetailUiIntent
    data object EditDeck : DeckDetailUiIntent
    data object DeleteDeck : DeckDetailUiIntent
    data object ConfirmDeleteDeck : DeckDetailUiIntent
    data object DismissDeleteDeck : DeckDetailUiIntent
    data class UndoDeleteCard(val flashcardId: String, val deletedAt: Long) : DeckDetailUiIntent
}

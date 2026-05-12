package com.emm.hello.newfeatures.deck

sealed interface DeckDetailUiIntent {
    data class SearchCardsChanged(val query: String) : DeckDetailUiIntent
    data object EditDeck : DeckDetailUiIntent
    data object DeleteDeck : DeckDetailUiIntent
    data object ConfirmDeleteDeck : DeckDetailUiIntent
    data object DismissDeleteDeck : DeckDetailUiIntent
}
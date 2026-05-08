package com.emm.hello.newfeatures.deck

sealed interface DeckDetailUiIntent {
    data class SearchCardsChanged(val query: String) : DeckDetailUiIntent
}

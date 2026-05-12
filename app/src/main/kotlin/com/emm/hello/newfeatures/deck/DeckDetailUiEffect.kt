package com.emm.hello.newfeatures.deck

sealed interface DeckDetailUiEffect {
    data class NavigateToEditDeck(val deckId: String) : DeckDetailUiEffect
    data object DeckDeleted : DeckDetailUiEffect
    data class ShowMessage(val message: String) : DeckDetailUiEffect
}
package com.emm.hello.newfeatures.deck

sealed interface DeckFormMode {
    data object Create : DeckFormMode
    data class Edit(val deckId: String) : DeckFormMode
}

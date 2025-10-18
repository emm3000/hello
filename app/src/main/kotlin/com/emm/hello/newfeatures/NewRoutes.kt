package com.emm.hello.newfeatures

import kotlinx.serialization.Serializable

object NewRoutes {

    @Serializable
    object Decks

    @Serializable
    data class DeckDetail(val deckId: String)

    @Serializable
    data class CardDetail(val cardId: String)
}
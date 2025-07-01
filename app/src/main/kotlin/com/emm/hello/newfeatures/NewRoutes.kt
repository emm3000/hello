package com.emm.hello.newfeatures

import kotlinx.serialization.Serializable

object NewRoutes {

    @Serializable
    object Dashboard

    @Serializable
    data class Study(val deckId: String)

    @Serializable
    object NewCard

    @Serializable
    object NewDeck

    @Serializable
    object Decks

    @Serializable
    object Quotes

    @Serializable
    data class DeckDetail(val deckId: String)

    @Serializable
    data class CardDetail(val cardId: String)
}
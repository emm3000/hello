package com.emm.domain.deck

interface DefaultDeckSelectionRepository {
    fun getDefaultDeckId(): String
    fun setDefaultDeckId(deckId: String)
}

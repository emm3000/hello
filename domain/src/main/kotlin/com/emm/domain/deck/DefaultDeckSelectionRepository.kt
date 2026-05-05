package com.emm.domain.deck

import com.emm.domain.ids.DeckId

interface DefaultDeckSelectionRepository {
    fun getDefaultDeckId(): String
    fun setDefaultDeckId(deckId: DeckId)
    fun clearDefaultDeckId()
}

package com.emm.domain.deck

import com.emm.domain.ids.DeckId

interface DefaultDeckSelectionRepository {
    fun getDefaultDeckId(): DeckId?
    fun setDefaultDeckId(deckId: DeckId?)
}

package com.emm.data.deck

import com.emm.data.remote.DataStore
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.ids.DeckId

class DefaultDeckSelectionPreferencesRepository(
    private val dataStore: DataStore,
) : DefaultDeckSelectionRepository {
    override fun getDefaultDeckId(): String {
        return dataStore.defaultDeck
    }

    override fun setDefaultDeckId(deckId: DeckId) {
        dataStore.defaultDeck = deckId.value
    }
}

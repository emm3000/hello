package com.emm.data.deck

import com.emm.data.remote.DataStore
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.ids.DeckId

class DefaultDeckSelectionPreferencesRepository(
    private val dataStore: DataStore,
) : DefaultDeckSelectionRepository {
    override fun getDefaultDeckId(): DeckId? {
        return dataStore.defaultDeck
            .takeUnless(String::isBlank)
            ?.let(DeckId::from)
    }

    override fun setDefaultDeckId(deckId: DeckId?) {
        if (deckId == null) {
            dataStore.clearDefaultDeck()
            return
        }

        dataStore.defaultDeck = deckId.value
    }
}

package com.emm.domain.deck

import com.emm.domain.ids.DeckId
import kotlinx.coroutines.flow.first

class SoftDeleteDeckUseCase(
    private val deckRepository: DeckRepository,
) {

    suspend operator fun invoke(deckId: DeckId): Long {
        val liveDecks: List<Deck> = deckRepository.fetchAll().first()
        if (liveDecks.none { it.id != deckId }) throw LastDeckDeletionException(deckId)
        return deckRepository.softDeleteDeck(deckId)
    }
}

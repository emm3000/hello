package com.emm.domain.deck

import com.emm.domain.ids.DeckId

class RestoreDeckUseCase(
    private val deckRepository: DeckRepository,
) {

    suspend operator fun invoke(deckId: DeckId, deletedAt: Long) {
        deckRepository.restoreDeck(deckId, deletedAt)
    }
}

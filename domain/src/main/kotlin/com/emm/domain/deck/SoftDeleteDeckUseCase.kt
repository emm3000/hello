package com.emm.domain.deck

import com.emm.domain.ids.DeckId

class SoftDeleteDeckUseCase(
    private val deckRepository: DeckRepository,
) {

    suspend operator fun invoke(deckId: DeckId) {
        deckRepository.softDeleteDeck(deckId)
    }
}

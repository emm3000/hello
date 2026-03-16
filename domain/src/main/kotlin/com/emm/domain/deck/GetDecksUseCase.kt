package com.emm.domain.deck

import kotlinx.coroutines.flow.Flow

class GetDecksUseCase(private val repository: DeckRepository) {

    operator fun invoke(): Flow<List<Deck>> {
        return repository.deckWithFlashcardCount()
    }
}

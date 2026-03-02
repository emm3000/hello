package com.emm.domain.deck

import kotlinx.coroutines.flow.Flow

class DeckFetcher(private val repository: DeckRepository) {

    fun fetch(): Flow<List<Deck>> {
        return repository.deckWithFlashcardCount()
    }
}

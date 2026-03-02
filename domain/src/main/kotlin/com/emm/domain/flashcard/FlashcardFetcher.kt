package com.emm.domain.flashcard

class FlashcardFetcher(private val repository: FlashcardRepository) {

    suspend fun fetchAll(deckId: String): List<Flashcard> {
        return repository.sessionToday(deckId)
    }
}

package com.emm.domain.flashcard

class FlashcardCreator(private val repository: FlashcardRepository) {

    suspend fun createFlashcard(word: String, deckId: String): String {
        return repository.create(word, deckId)
    }
}
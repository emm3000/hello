package com.emm.domain.flashcard

class FlashcardFinder(private val repository: FlashcardRepository) {

    suspend fun find(cardId: String): Flashcard = repository.fetchById(cardId)
}
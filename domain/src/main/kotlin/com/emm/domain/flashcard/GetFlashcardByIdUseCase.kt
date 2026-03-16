package com.emm.domain.flashcard

class GetFlashcardByIdUseCase(private val repository: FlashcardRepository) {

    suspend fun find(cardId: String): Flashcard = repository.fetchById(cardId)
}

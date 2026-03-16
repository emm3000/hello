package com.emm.domain.flashcard

class GetFlashcardByIdUseCase(private val repository: FlashcardReadRepository) {

    suspend operator fun invoke(cardId: String): Flashcard = repository.fetchById(cardId)
}

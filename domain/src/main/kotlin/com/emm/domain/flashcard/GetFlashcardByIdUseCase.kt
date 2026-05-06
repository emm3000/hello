package com.emm.domain.flashcard

import com.emm.domain.ids.toFlashcardId

class GetFlashcardByIdUseCase(private val repository: FlashcardReadRepository) {

    suspend operator fun invoke(flashcardId: String): FlashcardDetail {
        val typedCardId = flashcardId.toFlashcardId()
        return repository.fetchById(typedCardId)
    }
}

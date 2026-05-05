package com.emm.domain.flashcard

import com.emm.domain.ids.toFlashcardId

class GetFlashcardByIdUseCase(private val repository: FlashcardReadRepository) {

    suspend operator fun invoke(cardId: String): Flashcard {
        val typedCardId = cardId.toFlashcardId()
        return repository.fetchById(typedCardId.value)
    }
}

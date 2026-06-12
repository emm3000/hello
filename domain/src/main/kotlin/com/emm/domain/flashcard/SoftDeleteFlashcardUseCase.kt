package com.emm.domain.flashcard

import com.emm.domain.ids.FlashcardId

class SoftDeleteFlashcardUseCase(
    private val flashcardRepository: FlashcardRepository,
) {

    suspend operator fun invoke(flashcardId: FlashcardId): Long {
        return flashcardRepository.softDeleteFlashcard(flashcardId)
    }
}

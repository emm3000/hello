package com.emm.domain.flashcard

import com.emm.domain.ids.FlashcardId

class RestoreFlashcardUseCase(
    private val flashcardRepository: FlashcardRepository,
) {

    suspend operator fun invoke(flashcardId: FlashcardId, deletedAt: Long) {
        flashcardRepository.restoreFlashcard(flashcardId, deletedAt)
    }
}

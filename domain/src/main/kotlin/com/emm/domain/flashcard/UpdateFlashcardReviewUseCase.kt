package com.emm.domain.flashcard

class UpdateFlashcardReviewUseCase(private val repository: FlashcardReviewRepository) {

    suspend fun update(flashcardReview: FlashcardReview) {
        repository.update(flashcardReview)
    }
}

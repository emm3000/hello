package com.emm.domain.flashcard

class UpdateFlashcardReviewUseCase(private val repository: FlashcardReviewRepository) {

    suspend operator fun invoke(flashcardReview: FlashcardReview) {
        repository.update(flashcardReview)
    }
}

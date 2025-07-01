package com.emm.domain.flashcard

class FlashcardReviewUpdater(private val repository: FlashcardReviewRepository) {

    suspend fun update(flashcardReview: FlashcardReview) {
        repository.update(flashcardReview)
    }
}
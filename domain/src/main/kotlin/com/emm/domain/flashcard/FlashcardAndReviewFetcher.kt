package com.emm.domain.flashcard

class FlashcardAndReviewFetcher(private val repository: FlashcardRepository) {

    suspend fun fetch(deckId: String): List<Flashcard> {
        return repository.flashcardWithReview(deckId)
    }
}
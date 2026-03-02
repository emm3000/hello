package com.emm.domain.flashcard

import kotlinx.coroutines.flow.Flow

class FlashcardAndReviewFetcher(private val repository: FlashcardRepository) {

    fun fetch(deckId: String): Flow<List<Flashcard>> {
        return repository.flashcardWithReview(deckId)
    }
}

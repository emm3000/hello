package com.emm.domain.flashcard

import kotlinx.coroutines.flow.Flow

class ObserveFlashcardsWithReviewUseCase(private val repository: FlashcardRepository) {

    operator fun invoke(deckId: String): Flow<List<Flashcard>> {
        return repository.flashcardWithReview(deckId)
    }
}

package com.emm.domain.study

import com.emm.domain.ids.toDeckId
import kotlinx.coroutines.flow.Flow

class ObserveFlashcardsWithReviewUseCase(private val repository: StudySessionRepository) {

    operator fun invoke(deckId: String): Flow<List<StudyFlashcard>> {
        val typedDeckId = deckId.toDeckId()
        return repository.flashcardWithReview(typedDeckId)
    }
}

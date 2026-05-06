package com.emm.domain.flashcard

import com.emm.domain.study.StudySessionRepository
import com.emm.domain.ids.toDeckId
import kotlinx.coroutines.flow.Flow

class ObserveFlashcardsWithReviewUseCase(private val repository: StudySessionRepository) {

    operator fun invoke(deckId: String): Flow<List<Flashcard>> {
        val typedDeckId = deckId.toDeckId()
        return repository.flashcardWithReview(typedDeckId)
    }
}

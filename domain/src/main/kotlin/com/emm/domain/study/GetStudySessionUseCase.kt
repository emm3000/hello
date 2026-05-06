package com.emm.domain.study

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.ids.toDeckId

class GetStudySessionUseCase(private val repository: StudySessionRepository) {

    suspend operator fun invoke(deckId: String): List<Flashcard> {
        val typedDeckId = deckId.toDeckId()
        return repository.sessionToday(typedDeckId)
    }
}

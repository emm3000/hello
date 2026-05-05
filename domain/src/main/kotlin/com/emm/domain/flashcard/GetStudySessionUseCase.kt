package com.emm.domain.flashcard

import com.emm.domain.ids.toDeckId

class GetStudySessionUseCase(private val repository: StudySessionRepository) {

    suspend operator fun invoke(deckId: String): List<Flashcard> {
        val typedDeckId = deckId.toDeckId()
        return repository.sessionToday(typedDeckId.value)
    }
}

package com.emm.domain.study

import com.emm.domain.ids.toDeckId

class GetStudySessionUseCase(private val repository: StudySessionRepository) {

    suspend operator fun invoke(deckId: String): List<StudyFlashcard> {
        val typedDeckId = deckId.toDeckId()
        return repository.sessionToday(typedDeckId)
    }
}

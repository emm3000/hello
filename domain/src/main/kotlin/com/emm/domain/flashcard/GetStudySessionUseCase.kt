package com.emm.domain.flashcard

class GetStudySessionUseCase(private val repository: StudySessionRepository) {

    suspend operator fun invoke(deckId: String): List<Flashcard> {
        return repository.sessionToday(deckId)
    }
}

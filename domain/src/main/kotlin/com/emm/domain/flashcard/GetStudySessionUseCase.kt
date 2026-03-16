package com.emm.domain.flashcard

class GetStudySessionUseCase(private val repository: FlashcardRepository) {

    suspend operator fun invoke(deckId: String): List<Flashcard> {
        return repository.sessionToday(deckId)
    }
}

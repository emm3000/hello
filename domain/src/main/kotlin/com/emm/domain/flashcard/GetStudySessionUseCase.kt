package com.emm.domain.flashcard

class GetStudySessionUseCase(private val repository: FlashcardRepository) {

    suspend fun fetchAll(deckId: String): List<Flashcard> {
        return repository.sessionToday(deckId)
    }
}

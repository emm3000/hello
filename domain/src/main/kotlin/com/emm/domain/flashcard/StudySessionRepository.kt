package com.emm.domain.flashcard

import kotlinx.coroutines.flow.Flow

interface StudySessionRepository {
    suspend fun sessionToday(deckId: String): List<Flashcard>

    fun flashcardWithReview(deckId: String): Flow<List<Flashcard>>
}

package com.emm.domain.study

import com.emm.domain.ids.DeckId
import kotlinx.coroutines.flow.Flow

interface StudySessionRepository {
    suspend fun sessionToday(deckId: DeckId): List<StudyFlashcard>

    suspend fun sessionTodayAllDecks(): List<StudyFlashcard>

    fun flashcardWithReview(deckId: DeckId): Flow<List<StudyFlashcard>>
}

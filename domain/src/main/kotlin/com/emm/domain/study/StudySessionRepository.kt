package com.emm.domain.study

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.ids.DeckId
import kotlinx.coroutines.flow.Flow

interface StudySessionRepository {
    suspend fun sessionToday(deckId: DeckId): List<Flashcard>

    fun flashcardWithReview(deckId: DeckId): Flow<List<Flashcard>>
}

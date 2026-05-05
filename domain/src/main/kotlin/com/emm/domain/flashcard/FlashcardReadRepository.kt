package com.emm.domain.flashcard

import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import kotlinx.coroutines.flow.Flow

interface FlashcardReadRepository {
    fun fetchAll(): Flow<List<Flashcard>>

    fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>>

    suspend fun fetchById(id: FlashcardId): Flashcard
}

package com.emm.domain.flashcard

import kotlinx.coroutines.flow.Flow

interface FlashcardReadRepository {
    fun fetchAll(): Flow<List<Flashcard>>

    fun fetchByDeckId(deckId: String): Flow<List<Flashcard>>

    suspend fun fetchById(id: String): Flashcard
}

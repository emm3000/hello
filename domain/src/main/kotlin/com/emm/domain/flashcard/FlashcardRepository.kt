package com.emm.domain.flashcard

import kotlinx.coroutines.flow.Flow

interface FlashcardRepository {

    suspend fun create(word: String, deckId: String): String

    fun fetchAll(): Flow<List<Flashcard>>
}
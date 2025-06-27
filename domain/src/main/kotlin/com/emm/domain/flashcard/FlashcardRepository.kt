package com.emm.domain.flashcard

import kotlinx.coroutines.flow.Flow

interface FlashcardRepository {

    suspend fun create(input: CreateFlashcardInput): String

    suspend fun upsertExamples(examples: List<Example>, flashcardId: String)

    suspend fun generateFlashcard(word: String): FlashcardGenerated

    fun fetchAll(): Flow<List<Flashcard>>

    fun fetchByDeckId(deckId: String): Flow<List<Flashcard>>

    suspend fun fetchById(id: String): Flashcard
}
package com.emm.domain.flashcard

import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import kotlinx.coroutines.flow.Flow

interface FlashcardRepository {
    fun fetchAll(): Flow<List<Flashcard>>

    fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>>

    suspend fun fetchById(id: FlashcardId): FlashcardDetail

    suspend fun create(input: CreateFlashcardInput): FlashcardId

    suspend fun update(input: UpdateFlashcardInput)

    suspend fun softDeleteFlashcard(flashcardId: FlashcardId)

    suspend fun countDueFlashcards(nowMillis: Long): Long

    suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId)
}

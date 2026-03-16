package com.emm.domain.flashcard

interface FlashcardWriteRepository {
    suspend fun create(input: CreateFlashcardInput): String

    suspend fun upsertExamples(examples: List<Example>, flashcardId: String)
}

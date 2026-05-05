package com.emm.domain.flashcard

import com.emm.domain.ids.FlashcardId

interface FlashcardWriteRepository {
    suspend fun create(input: CreateFlashcardInput): FlashcardId

    suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId)
}

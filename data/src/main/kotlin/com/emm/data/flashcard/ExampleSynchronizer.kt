package com.emm.data.flashcard

import com.emm.data.FlashcardExample
import com.emm.data.FlashcardExampleQueries
import com.emm.data.HelloDb
import com.emm.data.deck.RemoteDataSource

class ExampleSynchronizer(
    db: HelloDb,
    private val remote: RemoteDataSource,
) {

    private val fq: FlashcardExampleQueries = db.flashcardExampleQueries

    suspend fun execute() {
        val flashcardExamples: List<FlashcardExample> = fq.pending().executeAsList()

        if (flashcardExamples.isEmpty()) return

        val newFlashcardRequests: List<CreateExampleRequest> = flashcardExamples.map(::toRequest)
        remote.createExample(newFlashcardRequests)
        val syncedFlashcardIds = flashcardExamples.map(FlashcardExample::id)
        fq.markAsSynced(syncedFlashcardIds)
    }
}

private fun toRequest(example: FlashcardExample): CreateExampleRequest = CreateExampleRequest(
    id = example.id,
    flashcardId = example.flashcardId,
    text = example.text,
    translation = example.translation,
    type = example.type,
    createdAt = example.createdAt,
    updatedAt = example.updatedAt,
)

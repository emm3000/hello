package com.emm.data.flashcard

import android.content.Context
import com.emm.data.Flashcard
import com.emm.data.FlashcardQueries
import com.emm.data.HelloDb
import com.emm.data.deck.RemoteDataSource

class FlashcardSynchronizer(
    db: HelloDb,
    private val remote: RemoteDataSource,
    private val context: Context,
) {

    private val fq: FlashcardQueries = db.flashcardQueries

    suspend fun execute() {
        val pendingFlashcards: List<FlashcardEntity> = fq.pending().executeAsList()

        if (pendingFlashcards.isEmpty()) return

        val newFlashcardRequests: List<CreateFlashcardRequest> = pendingFlashcards.map(::toRequest)
        remote.createFlashcard(newFlashcardRequests)
        val syncedFlashcardIds = pendingFlashcards.map(FlashcardEntity::id)
        fq.markAsSynced(syncedFlashcardIds)
    }

    fun synchronize() {
        FlashcardWorker.initialize(context)
    }
}

private fun toRequest(flashcard: Flashcard): CreateFlashcardRequest = CreateFlashcardRequest(
    id = flashcard.id,
    deckId = flashcard.deckId,
    word = flashcard.word,
    meaning = flashcard.meaning,
    translation = flashcard.translation.orEmpty(),
    phonetic = flashcard.phonetic.orEmpty(),
    note = flashcard.note.orEmpty(),
    createdAt = flashcard.createdAt,
    updatedAt = flashcard.updatedAt
)

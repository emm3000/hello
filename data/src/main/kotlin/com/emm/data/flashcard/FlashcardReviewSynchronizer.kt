package com.emm.data.flashcard

import android.content.Context
import com.emm.data.FlashcardReview
import com.emm.data.FlashcardReviewQueries
import com.emm.data.HelloDb
import com.emm.data.deck.RemoteDataSource

class FlashcardReviewSynchronizer(
    db: HelloDb,
    private val remote: RemoteDataSource,
    private val context: Context,
) {

    private val fq: FlashcardReviewQueries = db.flashcardReviewQueries

    suspend fun execute() {
        val flashcardReviews: List<FlashcardReview> = fq.pending().executeAsList()

        if (flashcardReviews.isEmpty()) return

        val newFlashcardRequests: List<CreateFlashcardReviewRequest> = flashcardReviews.map {
            CreateFlashcardReviewRequest(
                flashcardId = it.flashcardId,
                lastReviewedAt = it.lastReviewedAt ?: 0L,
                nextReviewAt = it.nextReviewAt ?: 0L,
                easeFactor = it.easeFactor,
                interval = it.interval,
                repetitions = it.repetitions,
                lapses = it.lapses,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
            )
        }
        remote.createReview(newFlashcardRequests)
        val syncedFlashcardIds = flashcardReviews.map(FlashcardReview::flashcardId)
        fq.markAsSynced(syncedFlashcardIds)
    }

    fun synchronize() {
        FlashcardReviewWorker.initialize(context)
    }
}
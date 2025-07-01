package com.emm.data.flashcard

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.HelloDb
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.FlashcardReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

typealias FlashcardReviewEntity = com.emm.data.FlashcardReview

class DefaultFlashcardReviewRepository(
    db: HelloDb,
) : FlashcardReviewRepository {

    private val dao = db.flashcardReviewQueries

    override fun all(): Flow<List<FlashcardReview>> {
        return dao
            .all()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(List<FlashcardReviewEntity>::toDomain)
    }

    override suspend fun update(flashcardReview: FlashcardReview) {
        dao.upsertFlashcardReview(
            flashcardId = flashcardReview.flashcardId,
            lastReviewedAt = flashcardReview.lastReviewedAt,
            nextReviewAt = flashcardReview.nextReviewAt,
            easeFactor = flashcardReview.easeFactor,
            interval = flashcardReview.interval,
            repetitions = flashcardReview.repetitions,
            lapses = flashcardReview.lapses,
        )
    }
}
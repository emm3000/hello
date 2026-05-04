package com.emm.data.flashcard

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.FlashcardReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
@LocalFirstWrite
class DefaultFlashcardReviewRepository(
    private val db: HelloDb,
) : FlashcardReviewRepository {

    private val localFirstQueries = db.localFirstQueries

    override fun all(): Flow<List<FlashcardReview>> {
        return localFirstQueries
            .allReviewProjections()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { projections -> projections.map { it.toDomainFromProjection() } }
    }

    override suspend fun update(flashcardReview: FlashcardReview) = withContext(Dispatchers.IO) {
        val now = Instant.now().toEpochMilli()
        val eventId = UUID.randomUUID().toString()

        db.transaction {
            localFirstQueries.insertReviewEvent(
                eventId = eventId,
                flashcardId = flashcardReview.flashcardId,
                grade = "review",
                reviewedAt = flashcardReview.lastReviewedAt,
                nextReviewAt = flashcardReview.nextReviewAt,
                easeFactor = flashcardReview.easeFactor,
                interval = flashcardReview.interval,
                repetitions = flashcardReview.repetitions,
                lapses = flashcardReview.lapses,
                createdAt = now,
            )
            localFirstQueries.upsertReviewProjection(
                flashcardId = flashcardReview.flashcardId,
                lastReviewedAt = flashcardReview.lastReviewedAt,
                nextReviewAt = flashcardReview.nextReviewAt,
                easeFactor = flashcardReview.easeFactor,
                interval = flashcardReview.interval,
                repetitions = flashcardReview.repetitions,
                lapses = flashcardReview.lapses,
                sourceEventId = eventId,
                updatedAt = now,
            )
        }
        Unit
    }
}

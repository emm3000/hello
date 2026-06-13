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
            .map { projections -> projections.map(ReviewProjectionEntity::toDomainFromProjection) }
    }

    override suspend fun update(flashcardReview: FlashcardReview) = withContext(Dispatchers.IO) {
        val now = Instant.now().toEpochMilli()
        val eventId = UUID.randomUUID().toString()

        db.transaction {
            localFirstQueries.insertReviewEvent(
                eventId = eventId,
                flashcardId = flashcardReview.flashcardId.value,
                grade = "review",
                reviewedAt = flashcardReview.lastReviewedAt,
                nextReviewAt = flashcardReview.nextReviewAt,
                easeFactor = flashcardReview.easeFactor,
                interval = flashcardReview.interval,
                repetitions = flashcardReview.repetitions,
                lapses = flashcardReview.lapses,
                createdAt = now,
                // TODO(PR3/T-17): replace 0 with real ReviewGrade ordinal (1..4) from FsrsCard scheduler.
                rating = 0L,
            )
            // Insert the row if it does not exist yet (brand-new card); defaults apply for state/stability/difficulty.
            // Then unconditionally update the SM-2/scheduling columns, leaving state/stability/difficulty untouched.
            // TODO(PR3/T-17): replace with a single full FSRS update once the scheduler provides real values.
            localFirstQueries.insertReviewProjectionIfAbsent(
                flashcardId = flashcardReview.flashcardId.value,
                lastReviewedAt = flashcardReview.lastReviewedAt,
                nextReviewAt = flashcardReview.nextReviewAt,
                easeFactor = flashcardReview.easeFactor,
                interval = flashcardReview.interval,
                repetitions = flashcardReview.repetitions,
                lapses = flashcardReview.lapses,
                sourceEventId = eventId,
                updatedAt = now,
            )
            localFirstQueries.updateReviewProjectionScheduling(
                lastReviewedAt = flashcardReview.lastReviewedAt,
                nextReviewAt = flashcardReview.nextReviewAt,
                easeFactor = flashcardReview.easeFactor,
                interval = flashcardReview.interval,
                repetitions = flashcardReview.repetitions,
                lapses = flashcardReview.lapses,
                sourceEventId = eventId,
                updatedAt = now,
                flashcardId = flashcardReview.flashcardId.value,
            )
        }
        Unit
    }
}

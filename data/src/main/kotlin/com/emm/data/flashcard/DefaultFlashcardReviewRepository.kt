package com.emm.data.flashcard

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.study.ReviewGrade
import com.emm.domain.study.toFsrsRating
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

    override fun all(): Flow<List<FsrsCard>> {
        return localFirstQueries
            .allReviewProjections()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { projections -> projections.map(ReviewProjectionEntity::toDomainFromProjection) }
    }

    override suspend fun update(card: FsrsCard, grade: ReviewGrade) = withContext(Dispatchers.IO) {
        val now = Instant.now().toEpochMilli()
        val eventId = UUID.randomUUID().toString()

        db.transaction {
            localFirstQueries.insertReviewEvent(
                eventId = eventId,
                flashcardId = card.flashcardId.value,
                grade = grade.name,
                reviewedAt = card.lastReviewedAt,
                nextReviewAt = card.nextReviewAt,
                easeFactor = LEGACY_EASE_FACTOR_PLACEHOLDER,
                interval = card.interval,
                repetitions = card.reps,
                lapses = card.lapses,
                createdAt = now,
                rating = grade.toFsrsRating().toLong(),
            )
            // Insert the row if it does not exist yet (brand-new card), then unconditionally
            // overwrite every FSRS column with the scheduler's freshly computed values. SQLite
            // UPSERT (ON CONFLICT) is unavailable on SQLDelight's bundled dialect, hence the pair.
            localFirstQueries.insertReviewProjectionFullIfAbsent(
                flashcardId = card.flashcardId.value,
                lastReviewedAt = card.lastReviewedAt,
                nextReviewAt = card.nextReviewAt,
                easeFactor = LEGACY_EASE_FACTOR_PLACEHOLDER,
                interval = card.interval,
                repetitions = card.reps,
                lapses = card.lapses,
                sourceEventId = eventId,
                updatedAt = now,
                state = card.state.name,
                stability = card.stability,
                difficulty = card.difficulty,
            )
            localFirstQueries.updateReviewProjectionFull(
                lastReviewedAt = card.lastReviewedAt,
                nextReviewAt = card.nextReviewAt,
                easeFactor = LEGACY_EASE_FACTOR_PLACEHOLDER,
                interval = card.interval,
                repetitions = card.reps,
                lapses = card.lapses,
                sourceEventId = eventId,
                updatedAt = now,
                state = card.state.name,
                stability = card.stability,
                difficulty = card.difficulty,
                flashcardId = card.flashcardId.value,
            )
        }
        Unit
    }

    private companion object {
        // Legacy SM-2 column kept for the seeding migration's read path; FSRS scheduling no
        // longer derives anything from it. A fixed mid-range placeholder avoids resurrecting
        // SM-2 semantics while keeping the NOT NULL legacy column populated.
        const val LEGACY_EASE_FACTOR_PLACEHOLDER = 2.5
    }
}

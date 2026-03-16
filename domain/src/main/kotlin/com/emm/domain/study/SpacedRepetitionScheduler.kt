package com.emm.domain.study

import com.emm.domain.flashcard.FlashcardReview
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToLong

enum class ReviewGrade { AGAIN, HARD, GOOD, EASY }

object SpacedRepetitionScheduler {

    private const val MINIMUM_EASE_FACTOR = 1.3
    private const val QUALITY_THRESHOLD_FOR_RESET = 3
    private const val MAX_QUALITY = 5
    private const val EASE_DELTA_BASE = 0.1
    private const val EASE_DELTA_FACTOR = 0.08
    private const val EASE_DELTA_PENALTY = 0.02
    private const val SECOND_REVIEW_INTERVAL_DAYS = 6L

    fun schedule(review: FlashcardReview, grade: ReviewGrade, flashcardId: String): FlashcardReview {
        val quality = when (grade) {
            ReviewGrade.AGAIN -> 1
            ReviewGrade.HARD -> 3
            ReviewGrade.GOOD -> 4
            ReviewGrade.EASY -> 5
        }

        val now = Instant.now()
        val newEaseFactor: Double
        val newRepetitions: Long
        val newInterval: Long
        val newLapses: Long

        if (quality < QUALITY_THRESHOLD_FOR_RESET) {
            newEaseFactor = review.easeFactor
            newRepetitions = 0
            newInterval = 1
            newLapses = review.lapses + 1
        } else {
            val qualityDistance = MAX_QUALITY - quality
            val easeAdjustment =
                EASE_DELTA_BASE - qualityDistance * (EASE_DELTA_FACTOR + qualityDistance * EASE_DELTA_PENALTY)
            newEaseFactor = max(
                MINIMUM_EASE_FACTOR,
                review.easeFactor + easeAdjustment
            )
            newRepetitions = review.repetitions + 1
            newInterval = when (newRepetitions) {
                1L -> 1
                2L -> SECOND_REVIEW_INTERVAL_DAYS
                else -> (review.interval * newEaseFactor).roundToLong()
            }
            newLapses = review.lapses
        }

        val newLastReviewedAt = now.toEpochMilli()
        val newNextReviewAt = now.plus(newInterval, ChronoUnit.DAYS).toEpochMilli()

        return review.copy(
            flashcardId = flashcardId,
            easeFactor = newEaseFactor,
            repetitions = newRepetitions,
            interval = newInterval,
            lapses = newLapses,
            lastReviewedAt = newLastReviewedAt,
            nextReviewAt = newNextReviewAt,
        )
    }
}

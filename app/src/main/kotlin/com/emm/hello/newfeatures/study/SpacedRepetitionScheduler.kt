package com.emm.hello.newfeatures.study

import com.emm.domain.flashcard.FlashcardReview
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToLong

enum class ReviewGrade { AGAIN, HARD, GOOD, EASY }

object SpacedRepetitionScheduler {

    private const val MINIMUM_EASE_FACTOR = 1.3

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

        if (quality < 3) {

            newEaseFactor = review.easeFactor
            newRepetitions = 0
            newInterval = 1
            newLapses = review.lapses + 1
        } else {
            newEaseFactor = max(
                MINIMUM_EASE_FACTOR,
                review.easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
            )
            newRepetitions = review.repetitions + 1
            newInterval = when (newRepetitions) {
                1L -> 1
                2L -> 6
                else -> (review.interval * newEaseFactor).roundToLong()
            }
            newLapses = review.lapses
        }

        val newLastReviewedAt = now.epochSecond
        val newNextReviewAt = now.plus(newInterval, ChronoUnit.DAYS).epochSecond

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
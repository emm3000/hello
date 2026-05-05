package com.emm.domain.flashcard

import com.emm.domain.ids.FlashcardId
import com.emm.domain.time.Clock

data class FlashcardReview(
    val flashcardId: FlashcardId,
    val lastReviewedAt: Long,
    val nextReviewAt: Long,
    val easeFactor: Double,
    val interval: Long,
    val repetitions: Long,
    val lapses: Long
) {

    init {
        require(easeFactor >= MIN_EASE_FACTOR) { "Ease factor cannot be below $MIN_EASE_FACTOR." }
        require(nextReviewAt >= lastReviewedAt) { "Next review timestamp cannot be before last reviewed timestamp." }
        require(interval >= 0L) { "Interval cannot be negative." }
        require(repetitions >= 0L) { "Repetitions cannot be negative." }
        require(lapses >= 0L) { "Lapses cannot be negative." }
    }

    companion object {

        private const val MIN_EASE_FACTOR = 1.3

        fun empty(clock: Clock): FlashcardReview {
            val now = clock.now().toEpochMilli()

            return FlashcardReview(
                flashcardId = FlashcardId.from("empty-flashcard"),
                lastReviewedAt = now,
                nextReviewAt = now,
                easeFactor = 2.5,
                interval = 0L,
                repetitions = 0L,
                lapses = 0L,
            )
        }
    }
}

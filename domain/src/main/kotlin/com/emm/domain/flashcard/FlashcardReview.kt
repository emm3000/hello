package com.emm.domain.flashcard

import com.emm.domain.time.Clock
import com.emm.domain.time.SystemClock

data class FlashcardReview(
    val flashcardId: String,
    val lastReviewedAt: Long,
    val nextReviewAt: Long,
    val easeFactor: Double,
    val interval: Long,
    val repetitions: Long,
    val lapses: Long
) {

    companion object {

        fun empty(clock: Clock): FlashcardReview {
            val now = clock.now().toEpochMilli()

            return FlashcardReview(
                flashcardId = "",
                lastReviewedAt = now,
                nextReviewAt = now,
                easeFactor = 2.5,
                interval = 0L,
                repetitions = 0L,
                lapses = 0L,
            )
        }

        @Deprecated(
            message = "Use empty(clock) for deterministic time.",
            replaceWith = ReplaceWith(
                expression = "empty(SystemClock)",
                imports = ["com.emm.domain.time.SystemClock"],
            ),
        )
        val Empty get() = empty(SystemClock)
    }
}

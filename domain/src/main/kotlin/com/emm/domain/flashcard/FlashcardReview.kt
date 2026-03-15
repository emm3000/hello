package com.emm.domain.flashcard

import java.time.Instant

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

        val Empty get() = FlashcardReview(
            flashcardId = "",
            lastReviewedAt = Instant.now().toEpochMilli(),
            nextReviewAt = Instant.now().toEpochMilli(),
            easeFactor = 2.5,
            interval = 0L,
            repetitions = 0L,
            lapses = 0L
        )
    }
}

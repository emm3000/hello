package com.emm.domain.flashcard

import com.emm.domain.ids.FlashcardId
import com.emm.domain.time.Clock

data class FsrsCard(
    val flashcardId: FlashcardId,
    val state: FsrsState,
    val stability: Double,
    val difficulty: Double,
    val lastReviewedAt: Long,
    val nextReviewAt: Long,
    val interval: Long,
    val reps: Long,
    val lapses: Long,
    val productionSince: Long? = null,
) {

    init {
        require(stability >= 0.0) { "Stability must be non-negative, was $stability." }
        require(state == FsrsState.NEW || difficulty in MIN_DIFFICULTY..MAX_DIFFICULTY) {
            "Difficulty must be in [$MIN_DIFFICULTY, $MAX_DIFFICULTY] for non-NEW cards, was $difficulty."
        }
        require(nextReviewAt >= lastReviewedAt) {
            "nextReviewAt ($nextReviewAt) cannot be before lastReviewedAt ($lastReviewedAt)."
        }
        require(interval >= 0L) { "Interval must be non-negative, was $interval." }
        require(reps >= 0L) { "Reps must be non-negative, was $reps." }
        require(lapses >= 0L) { "Lapses must be non-negative, was $lapses." }
    }

    companion object {

        private const val MIN_DIFFICULTY = 1.0
        private const val MAX_DIFFICULTY = 10.0

        fun new(flashcardId: FlashcardId, clock: Clock): FsrsCard {
            val nowMs = clock.now().toEpochMilli()
            return FsrsCard(
                flashcardId = flashcardId,
                state = FsrsState.NEW,
                stability = 0.0,
                difficulty = 0.0,
                lastReviewedAt = nowMs,
                nextReviewAt = nowMs,
                interval = 0L,
                reps = 0L,
                lapses = 0L,
            )
        }
    }
}

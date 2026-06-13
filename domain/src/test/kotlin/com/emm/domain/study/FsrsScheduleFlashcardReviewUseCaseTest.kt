package com.emm.domain.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsParameters
import com.emm.domain.flashcard.FsrsState
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FsrsScheduleFlashcardReviewUseCaseTest {

    private val fixedNow = Instant.parse("2026-06-12T12:00:00Z")
    private val useCase = FsrsScheduleFlashcardReviewUseCase(Clock { fixedNow })

    @Test
    fun `invoke schedules a NEW card with GOOD and returns LEARNING state`() {
        val card = FsrsCard.new("card-1".toFlashcardId(), Clock { fixedNow })

        val result = useCase(card, ReviewGrade.GOOD, "card-1".toFlashcardId())

        assertEquals(FsrsState.LEARNING, result.state)
        assertTrue(result.stability > 0.0)
    }

    @Test
    fun `invoke schedules a REVIEW card with AGAIN and returns RELEARNING`() {
        val card = reviewCard("card-2".toFlashcardId())

        val result = useCase(card, ReviewGrade.AGAIN, "card-2".toFlashcardId())

        assertEquals(FsrsState.RELEARNING, result.state)
        assertEquals(1L, result.lapses)
    }

    @Test
    fun `invoke sets lastReviewedAt to clock now`() {
        val card = FsrsCard.new("card-3".toFlashcardId(), Clock { fixedNow })

        val result = useCase(card, ReviewGrade.GOOD, "card-3".toFlashcardId())

        assertEquals(fixedNow.toEpochMilli(), result.lastReviewedAt)
    }

    @Test
    fun `invoke replaces flashcardId with the provided id`() {
        val card = FsrsCard.new("original".toFlashcardId(), Clock { fixedNow })

        val result = useCase(card, ReviewGrade.GOOD, "new-id".toFlashcardId())

        assertEquals("new-id", result.flashcardId.value)
    }

    @Test
    fun `invoke uses DEFAULT params when none provided`() {
        val card = FsrsCard.new("card-4".toFlashcardId(), Clock { fixedNow })

        val resultDefault = useCase(card, ReviewGrade.GOOD, "card-4".toFlashcardId())
        val resultExplicit = FsrsScheduler.schedule(
            card = card,
            grade = ReviewGrade.GOOD,
            flashcardId = "card-4".toFlashcardId(),
            clock = Clock { fixedNow },
            params = FsrsParameters.DEFAULT,
        )

        assertEquals(resultExplicit.stability, resultDefault.stability, 1e-9)
        assertEquals(resultExplicit.interval, resultDefault.interval)
    }

    private fun reviewCard(id: com.emm.domain.ids.FlashcardId) = FsrsCard(
        flashcardId = id,
        state = FsrsState.REVIEW,
        stability = 10.0,
        difficulty = 5.0,
        lastReviewedAt = fixedNow.toEpochMilli() - 10 * 86_400_000L,
        nextReviewAt = fixedNow.toEpochMilli(),
        interval = 10L,
        reps = 3L,
        lapses = 0L,
    )
}

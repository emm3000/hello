package com.emm.domain.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsParameters
import com.emm.domain.flashcard.FsrsState
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ScheduleFlashcardReviewUseCaseTest {

    private val fixedNow = Instant.parse("2026-06-12T12:00:00Z")
    private val useCase = ScheduleFlashcardReviewUseCase(Clock { fixedNow })

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

    @Test
    fun `invoke stamps productionSince with clock now when a REVIEW card crosses stability 21`() {
        val card = reviewCard("card-5".toFlashcardId(), stability = 10.0)

        val result = useCase(card, ReviewGrade.GOOD, "card-5".toFlashcardId())

        assertTrue(result.stability >= 21.0)
        assertEquals(fixedNow.toEpochMilli(), result.productionSince)
    }

    @Test
    fun `invoke keeps the original productionSince on a later review after graduation`() {
        val laterNow = fixedNow.plusSeconds(30 * 86_400L)
        val graduationUseCase = ScheduleFlashcardReviewUseCase(Clock { fixedNow })
        val laterUseCase = ScheduleFlashcardReviewUseCase(Clock { laterNow })
        val card = reviewCard("card-6".toFlashcardId(), stability = 10.0)

        val graduated = graduationUseCase(card, ReviewGrade.GOOD, "card-6".toFlashcardId())
        val laterResult = laterUseCase(graduated, ReviewGrade.GOOD, "card-6".toFlashcardId())

        assertEquals(fixedNow.toEpochMilli(), laterResult.productionSince)
    }

    @Test
    fun `invoke keeps productionSince when AGAIN moves a graduated card to RELEARNING`() {
        val originalProductionSince: Long = fixedNow.toEpochMilli() - 30 * 86_400_000L
        val graduatedCard = reviewCard("card-7".toFlashcardId(), stability = 10.0)
            .copy(productionSince = originalProductionSince)

        val result = useCase(graduatedCard, ReviewGrade.AGAIN, "card-7".toFlashcardId())

        assertEquals(FsrsState.RELEARNING, result.state)
        assertEquals(originalProductionSince, result.productionSince)
    }

    @Test
    fun `invoke leaves productionSince null when a REVIEW card lands below stability 21`() {
        val card = reviewCard("card-8".toFlashcardId(), stability = 1.0)

        val result = useCase(card, ReviewGrade.GOOD, "card-8".toFlashcardId())

        assertTrue(result.stability < 21.0)
        assertEquals(null, result.productionSince)
    }

    @Test
    fun `invoke never stamps productionSince for a NEW card regardless of grade`() {
        val card = FsrsCard.new("card-9".toFlashcardId(), Clock { fixedNow })

        val result = useCase(card, ReviewGrade.EASY, "card-9".toFlashcardId())

        assertEquals(FsrsState.LEARNING, result.state)
        assertEquals(null, result.productionSince)
    }

    @Test
    fun `invoke stamps productionSince with the same instant it stamped lastReviewedAt on graduation`() {
        var elapsedMillis: Long = 0L
        val advancingClock = Clock {
            elapsedMillis += 1_000L
            fixedNow.plusMillis(elapsedMillis)
        }
        val advancingUseCase = ScheduleFlashcardReviewUseCase(advancingClock)
        val card = reviewCard("card-10".toFlashcardId(), stability = 10.0)

        val result = advancingUseCase(card, ReviewGrade.GOOD, "card-10".toFlashcardId())

        assertEquals(result.lastReviewedAt, result.productionSince)
    }

    private fun reviewCard(id: FlashcardId, stability: Double = 10.0): FsrsCard = FsrsCard(
        flashcardId = id,
        state = FsrsState.REVIEW,
        stability = stability,
        difficulty = 5.0,
        lastReviewedAt = fixedNow.toEpochMilli() - 10 * 86_400_000L,
        nextReviewAt = fixedNow.toEpochMilli(),
        interval = 10L,
        reps = 3L,
        lapses = 0L,
    )
}

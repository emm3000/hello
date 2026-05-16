package com.emm.domain.study

import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ScheduleFlashcardReviewUseCaseTest {

    private val fixedNow = Instant.parse("2026-05-04T12:30:45Z")
    private val useCase = ScheduleFlashcardReviewUseCase(Clock { fixedNow })

    @Test
    fun `invoke when again resets repetitions and sets one day interval`() {
        val review = baseReview().copy(
            easeFactor = 2.1,
            repetitions = 7L,
            interval = 20L,
            lapses = 2L,
        )

        val result = useCase(review = review, grade = ReviewGrade.AGAIN, flashcardId = "card-1".toFlashcardId())

        assertEquals("card-1", result.flashcardId.value)
        assertEquals(review.easeFactor, result.easeFactor, 0.0001)
        assertEquals(0L, result.repetitions)
        assertEquals(1L, result.interval)
        assertEquals(3L, result.lapses)
        assertEquals(fixedNow.toEpochMilli(), result.lastReviewedAt)
        assertEquals(86_400_000L, result.nextReviewAt - result.lastReviewedAt)
    }

    @Test
    fun `invoke when good and second repetition sets six day interval`() {
        val review = baseReview().copy(
            easeFactor = 2.5,
            repetitions = 1L,
            interval = 1L,
        )

        val result = useCase(review = review, grade = ReviewGrade.GOOD, flashcardId = "card-2".toFlashcardId())

        assertEquals("card-2", result.flashcardId.value)
        assertEquals(2L, result.repetitions)
        assertEquals(6L, result.interval)
        assertEquals(2.5, result.easeFactor, 0.0001)
        assertEquals(fixedNow.toEpochMilli(), result.lastReviewedAt)
        assertEquals(6L * 86_400_000L, result.nextReviewAt - result.lastReviewedAt)
    }

    @Test
    fun `invoke when easy after second review increases ease and rounds interval`() {
        val review = baseReview().copy(
            easeFactor = 2.5,
            repetitions = 2L,
            interval = 6L,
            lapses = 1L,
        )

        val result = useCase(review = review, grade = ReviewGrade.EASY, flashcardId = "card-3".toFlashcardId())

        assertEquals("card-3", result.flashcardId.value)
        assertEquals(3L, result.repetitions)
        assertEquals(16L, result.interval)
        assertEquals(2.6, result.easeFactor, 0.0001)
        assertEquals(1L, result.lapses)
        assertEquals(fixedNow.toEpochMilli(), result.lastReviewedAt)
        assertEquals(16L * 86_400_000L, result.nextReviewAt - result.lastReviewedAt)
    }

    @Test
    fun `invoke when hard at minimum ease keeps ease at exactly 1,3 (floor enforced)`() {
        val review = baseReview().copy(
            easeFactor = 1.3,
            repetitions = 5L,
            interval = 8L,
        )

        val result = useCase(review = review, grade = ReviewGrade.HARD, flashcardId = "card-4".toFlashcardId())

        assertEquals("card-4", result.flashcardId.value)
        assertEquals(1.3, result.easeFactor, 0.0001)
    }

    @Test
    fun `invoke always schedules next review after last reviewed time`() {
        val review = baseReview().copy(
            easeFactor = 2.2,
            repetitions = 3L,
            interval = 10L,
            lapses = 1L,
        )

        val result = useCase(review = review, grade = ReviewGrade.GOOD, flashcardId = "card-5".toFlashcardId())

        assertEquals("card-5", result.flashcardId.value)
        assertTrue(result.nextReviewAt > result.lastReviewedAt)
    }

    private fun baseReview(): FlashcardReview = FlashcardReview(
        flashcardId = "base".toFlashcardId(),
        lastReviewedAt = 0L,
        nextReviewAt = 0L,
        easeFactor = 2.5,
        interval = 0L,
        repetitions = 0L,
        lapses = 0L,
    )
}

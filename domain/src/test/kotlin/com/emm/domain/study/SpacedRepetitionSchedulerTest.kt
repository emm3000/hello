package com.emm.domain.study

import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.Clock
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedRepetitionSchedulerTest {

    private val now = Instant.parse("2026-05-15T12:00:00Z")
    private val clock = Clock { now }
    private val flashcardId = "card-1".toFlashcardId()

    @Test
    fun `AGAIN resets repetitions to zero, sets interval to 1 day, increments lapses`() {
        val review = newCard().copy(repetitions = 8L, interval = 30L, lapses = 2L)

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.AGAIN, flashcardId, clock)

        assertEquals(0L, result.repetitions)
        assertEquals(1L, result.interval)
        assertEquals(3L, result.lapses)
    }

    @Test
    fun `AGAIN does not change ease factor`() {
        val review = newCard().copy(easeFactor = 2.4)

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.AGAIN, flashcardId, clock)

        assertEquals(2.4, result.easeFactor, EPSILON)
    }

    @Test
    fun `HARD passes but decreases ease factor by 0,14`() {
        val review = newCard().copy(easeFactor = 2.5, repetitions = 3L, interval = 10L)

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.HARD, flashcardId, clock)

        assertEquals(2.36, result.easeFactor, EPSILON)
        assertEquals(4L, result.repetitions)
    }

    @Test
    fun `GOOD passes and leaves ease factor unchanged`() {
        val review = newCard().copy(easeFactor = 2.5, repetitions = 3L, interval = 10L)

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)

        assertEquals(2.5, result.easeFactor, EPSILON)
        assertEquals(4L, result.repetitions)
    }

    @Test
    fun `EASY passes and increases ease factor by 0,10`() {
        val review = newCard().copy(easeFactor = 2.5, repetitions = 3L, interval = 10L)

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.EASY, flashcardId, clock)

        assertEquals(2.6, result.easeFactor, EPSILON)
        assertEquals(4L, result.repetitions)
    }

    @Test
    fun `HARD at ease 1,3 keeps ease at 1,3 (floor)`() {
        val review = newCard().copy(easeFactor = 1.3, repetitions = 5L, interval = 10L)

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.HARD, flashcardId, clock)

        assertEquals(1.3, result.easeFactor, EPSILON)
    }

    @Test
    fun `repeated HARDs eventually floor at 1,3`() {
        var review = newCard().copy(easeFactor = 1.8, repetitions = 5L, interval = 10L)
        repeat(10) {
            review = SpacedRepetitionScheduler.schedule(review, ReviewGrade.HARD, flashcardId, clock)
        }

        assertEquals(1.3, review.easeFactor, EPSILON)
    }

    @Test
    fun `HARD at ease 1,4 floors to 1,3 (would-be 1,26)`() {
        val review = newCard().copy(easeFactor = 1.4, repetitions = 5L, interval = 10L)

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.HARD, flashcardId, clock)

        assertEquals(1.3, result.easeFactor, EPSILON)
    }

    @Test
    fun `first successful repetition (repetitions 0 to 1) sets interval to 1 day`() {
        val review = newCard().copy(repetitions = 0L, interval = 0L)

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)

        assertEquals(1L, result.repetitions)
        assertEquals(1L, result.interval)
    }

    @Test
    fun `second successful repetition (repetitions 1 to 2) sets interval to 6 days`() {
        val review = newCard().copy(repetitions = 1L, interval = 1L)

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)

        assertEquals(2L, result.repetitions)
        assertEquals(6L, result.interval)
    }

    @Test
    fun `third successful repetition multiplies interval by new ease (rounded)`() {
        val review = newCard().copy(easeFactor = 2.5, repetitions = 2L, interval = 6L)

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)

        assertEquals(3L, result.repetitions)
        assertEquals(15L, result.interval)
    }

    @Test
    fun `interval rounding uses banker rounding via roundToLong`() {
        val review = newCard().copy(easeFactor = 2.5, repetitions = 5L, interval = 7L)

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)

        assertEquals(18L, result.interval)
    }

    @Test
    fun `multiple AGAINs accumulate lapses`() {
        var review = newCard().copy(repetitions = 5L, interval = 30L, lapses = 0L)
        repeat(3) {
            review = SpacedRepetitionScheduler.schedule(review, ReviewGrade.AGAIN, flashcardId, clock)
        }

        assertEquals(3L, review.lapses)
        assertEquals(0L, review.repetitions)
        assertEquals(1L, review.interval)
    }

    @Test
    fun `successful grades do not increment lapses`() {
        val review = newCard().copy(repetitions = 3L, interval = 10L, lapses = 4L)

        val good = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)
        val easy = SpacedRepetitionScheduler.schedule(review, ReviewGrade.EASY, flashcardId, clock)
        val hard = SpacedRepetitionScheduler.schedule(review, ReviewGrade.HARD, flashcardId, clock)

        assertEquals(4L, good.lapses)
        assertEquals(4L, easy.lapses)
        assertEquals(4L, hard.lapses)
    }

    @Test
    fun `brand new card with AGAIN gets lapses 1, repetitions 0, interval 1`() {
        val review = newCard()

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.AGAIN, flashcardId, clock)

        assertEquals(1L, result.lapses)
        assertEquals(0L, result.repetitions)
        assertEquals(1L, result.interval)
    }

    @Test
    fun `brand new card with EASY jumps to ease 2,6 and 1-day interval`() {
        val review = newCard()

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.EASY, flashcardId, clock)

        assertEquals(2.6, result.easeFactor, EPSILON)
        assertEquals(1L, result.repetitions)
        assertEquals(1L, result.interval)
    }

    @Test
    fun `GOOD GOOD GOOD sequence reaches expected ease and interval`() {
        var review = newCard()
        review = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)
        review = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)
        review = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)

        assertEquals(2.5, review.easeFactor, EPSILON)
        assertEquals(3L, review.repetitions)
        assertEquals(15L, review.interval)
        assertEquals(0L, review.lapses)
    }

    @Test
    fun `GOOD then HARD then GOOD sees ease drop then stabilize`() {
        var review = newCard().copy(repetitions = 2L, interval = 6L, easeFactor = 2.5)
        review = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)
        assertEquals(2.5, review.easeFactor, EPSILON)

        review = SpacedRepetitionScheduler.schedule(review, ReviewGrade.HARD, flashcardId, clock)
        assertEquals(2.36, review.easeFactor, EPSILON)

        review = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)
        assertEquals(2.36, review.easeFactor, EPSILON)
    }

    @Test
    fun `lapse mid-stream resets repetitions but keeps ease`() {
        var review = newCard()
        review = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)
        review = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)
        review = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)
        val easeBeforeLapse = review.easeFactor

        review = SpacedRepetitionScheduler.schedule(review, ReviewGrade.AGAIN, flashcardId, clock)

        assertEquals(0L, review.repetitions)
        assertEquals(1L, review.interval)
        assertEquals(1L, review.lapses)
        assertEquals(easeBeforeLapse, review.easeFactor, EPSILON)
    }

    @Test
    fun `lastReviewedAt is set to clock now`() {
        val review = newCard()

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)

        assertEquals(now.toEpochMilli(), result.lastReviewedAt)
    }

    @Test
    fun `nextReviewAt is interval days after now`() {
        val review = newCard().copy(easeFactor = 2.5, repetitions = 2L, interval = 6L)

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)

        val expectedNext = now.toEpochMilli() + result.interval * MILLIS_PER_DAY
        assertEquals(expectedNext, result.nextReviewAt)
    }

    @Test
    fun `nextReviewAt always strictly after lastReviewedAt for non-failure`() {
        val review = newCard().copy(easeFactor = 2.5, repetitions = 5L, interval = 20L)

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, flashcardId, clock)

        assertTrue(result.nextReviewAt > result.lastReviewedAt)
    }

    @Test
    fun `flashcardId is replaced by the argument, not taken from review`() {
        val review = newCard()
        val targetId = "different-card".toFlashcardId()

        val result = SpacedRepetitionScheduler.schedule(review, ReviewGrade.GOOD, targetId, clock)

        assertEquals("different-card", result.flashcardId.value)
    }

    private fun newCard(): FlashcardReview = FlashcardReview(
        flashcardId = flashcardId,
        lastReviewedAt = now.toEpochMilli(),
        nextReviewAt = now.toEpochMilli(),
        easeFactor = 2.5,
        interval = 0L,
        repetitions = 0L,
        lapses = 0L,
    )

    private companion object {
        const val EPSILON = 0.0001
        const val MILLIS_PER_DAY = 86_400_000L
    }
}

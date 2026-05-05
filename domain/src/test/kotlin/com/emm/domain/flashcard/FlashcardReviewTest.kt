package com.emm.domain.flashcard

import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class FlashcardReviewTest {

    @Test
    fun `empty uses injected clock for timestamps`() {
        val instant = Instant.parse("2026-05-04T12:30:45Z")

        val review = FlashcardReview.empty(Clock { instant })

        assertEquals(instant.toEpochMilli(), review.lastReviewedAt)
        assertEquals(instant.toEpochMilli(), review.nextReviewAt)
        assertEquals(2.5, review.easeFactor, 0.0)
        assertEquals(0L, review.interval)
        assertEquals(0L, review.repetitions)
        assertEquals(0L, review.lapses)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects ease factor below minimum`() {
        FlashcardReview(
            flashcardId = "card-1".toFlashcardId(),
            lastReviewedAt = 10L,
            nextReviewAt = 20L,
            easeFactor = 1.2,
            interval = 0L,
            repetitions = 0L,
            lapses = 0L,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects next review before last reviewed`() {
        FlashcardReview(
            flashcardId = "card-1".toFlashcardId(),
            lastReviewedAt = 20L,
            nextReviewAt = 10L,
            easeFactor = 2.5,
            interval = 0L,
            repetitions = 0L,
            lapses = 0L,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative interval`() {
        FlashcardReview(
            flashcardId = "card-1".toFlashcardId(),
            lastReviewedAt = 10L,
            nextReviewAt = 20L,
            easeFactor = 2.5,
            interval = -1L,
            repetitions = 0L,
            lapses = 0L,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative repetitions`() {
        FlashcardReview(
            flashcardId = "card-1".toFlashcardId(),
            lastReviewedAt = 10L,
            nextReviewAt = 20L,
            easeFactor = 2.5,
            interval = 0L,
            repetitions = -1L,
            lapses = 0L,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative lapses`() {
        FlashcardReview(
            flashcardId = "card-1".toFlashcardId(),
            lastReviewedAt = 10L,
            nextReviewAt = 20L,
            easeFactor = 2.5,
            interval = 0L,
            repetitions = 0L,
            lapses = -1L,
        )
    }
}

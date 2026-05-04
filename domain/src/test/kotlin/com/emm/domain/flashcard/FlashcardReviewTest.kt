package com.emm.domain.flashcard

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
}

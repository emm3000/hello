package com.emm.data.flashcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlashcardMappersTest {

    @Test
    fun `toDomainFromProjection carries a non-null productionSince through to FsrsCard`() {
        val projection: ReviewProjectionEntity = reviewProjection(productionSince = 5_000_000L)

        val card = projection.toDomainFromProjection()

        assertEquals(5_000_000L, card.productionSince)
    }

    @Test
    fun `toDomainFromProjection carries a null productionSince through to FsrsCard`() {
        val projection: ReviewProjectionEntity = reviewProjection(productionSince = null)

        val card = projection.toDomainFromProjection()

        assertNull(card.productionSince)
    }

    private fun reviewProjection(productionSince: Long?): ReviewProjectionEntity = ReviewProjectionEntity(
        flashcardId = "card-1",
        lastReviewedAt = 1_000L,
        nextReviewAt = 2_000L,
        easeFactor = 2.5,
        interval = 1L,
        repetitions = 1L,
        lapses = 0L,
        sourceEventId = "event-1",
        updatedAt = 2_000L,
        state = "REVIEW",
        stability = 1.0,
        difficulty = 5.0,
        productionSince = productionSince,
    )
}

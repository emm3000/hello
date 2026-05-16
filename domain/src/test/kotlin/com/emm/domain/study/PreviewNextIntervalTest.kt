package com.emm.domain.study

import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.Clock
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewNextIntervalTest {

    private val now = Instant.parse("2026-05-15T12:00:00Z")
    private val clock = Clock { now }
    private val flashcardId = "card-1".toFlashcardId()

    @Test
    fun `previewAll returns one entry per ReviewGrade`() {
        val preview = PreviewNextInterval.previewAll(newCard(), clock)

        assertEquals(ReviewGrade.entries.size, preview.size)
        ReviewGrade.entries.forEach { grade ->
            assertTrue("Missing preview for $grade", preview.containsKey(grade))
        }
    }

    @Test
    fun `previewAll for a fresh card produces interval of 1 day for AGAIN`() {
        val preview = PreviewNextInterval.previewAll(newCard(), clock)

        assertEquals(1L, preview[ReviewGrade.AGAIN])
    }

    @Test
    fun `previewAll for a mature card preserves SRS scaling order GOOD greater than HARD`() {
        val mature = newCard().copy(
            easeFactor = 2.5,
            repetitions = 5L,
            interval = 14L,
        )

        val preview = PreviewNextInterval.previewAll(mature, clock)

        val again = preview.getValue(ReviewGrade.AGAIN)
        val hard = preview.getValue(ReviewGrade.HARD)
        val good = preview.getValue(ReviewGrade.GOOD)
        val easy = preview.getValue(ReviewGrade.EASY)

        assertEquals(1L, again)
        assertTrue("HARD ($hard) should be greater than AGAIN ($again)", hard > again)
        assertTrue("GOOD ($good) should be greater or equal to HARD ($hard)", good >= hard)
        assertTrue("EASY ($easy) should be greater or equal to GOOD ($good)", easy >= good)
    }

    @Test
    fun `previewAll does not mutate the original review`() {
        val original = newCard().copy(repetitions = 3L, interval = 6L, easeFactor = 2.4)
        val snapshot = original.copy()

        PreviewNextInterval.previewAll(original, clock)

        assertEquals(snapshot, original)
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
}

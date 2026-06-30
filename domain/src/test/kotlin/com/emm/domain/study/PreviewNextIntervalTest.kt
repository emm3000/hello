package com.emm.domain.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsParameters
import com.emm.domain.flashcard.FsrsState
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PreviewNextIntervalTest {

    private val now = Instant.parse("2026-06-12T12:00:00Z")
    private val clock = Clock { now }
    private val flashcardId = "card-1".toFlashcardId()
    private val params = FsrsParameters.DEFAULT

    @Test
    fun `previewAll returns one entry per ReviewGrade`() {
        val card = FsrsCard.new(flashcardId, clock)

        val preview = PreviewNextInterval.previewAll(card, clock, params)

        assertEquals(ReviewGrade.entries.size, preview.size)
        ReviewGrade.entries.forEach { grade ->
            assertNotNull("Missing preview for $grade", preview[grade])
        }
    }

    @Test
    fun `previewAll for a new card has all four grades and strictly ordered stability values`() {
        // Intervals may legitimately collide at the 1-day floor (S0(AGAIN)=0.212 and
        // S0(HARD)=1.2931 both round to interval=1 with canonical weights). The
        // behavioral invariant is on STABILITY, not the integer interval.
        val card = FsrsCard.new(flashcardId, clock)

        val preview = PreviewNextInterval.previewAll(card, clock, params)

        // All 4 grades must be present in the map.
        ReviewGrade.entries.forEach { grade ->
            assertNotNull("Missing preview for $grade", preview[grade])
        }

        // Stability ordering must be strictly EASY > GOOD > HARD > AGAIN.
        val sAgain = FsrsScheduler.schedule(card, ReviewGrade.AGAIN, flashcardId, clock, params).stability
        val sHard = FsrsScheduler.schedule(card, ReviewGrade.HARD, flashcardId, clock, params).stability
        val sGood = FsrsScheduler.schedule(card, ReviewGrade.GOOD, flashcardId, clock, params).stability
        val sEasy = FsrsScheduler.schedule(card, ReviewGrade.EASY, flashcardId, clock, params).stability

        val stabilities = listOf(sAgain, sHard, sGood, sEasy)
        assertEquals("All four stability values must be distinct", 4, stabilities.distinct().size)
        assertTrue("EASY stability($sEasy) > GOOD($sGood)", sEasy > sGood)
        assertTrue("GOOD stability($sGood) > HARD($sHard)", sGood > sHard)
        assertTrue("HARD stability($sHard) > AGAIN($sAgain)", sHard > sAgain)
    }

    @Test
    fun `previewAll ordering non-strict EASY ge GOOD ge HARD ge AGAIN intervals, strict on stability`() {
        // Intervals are non-strictly ordered (floor at 1 day may cause ties).
        // Stability is strictly ordered by canonical FSRS-6 design.
        val card = FsrsCard.new(flashcardId, clock)

        val preview = PreviewNextInterval.previewAll(card, clock, params)

        val again = preview.getValue(ReviewGrade.AGAIN)
        val hard = preview.getValue(ReviewGrade.HARD)
        val good = preview.getValue(ReviewGrade.GOOD)
        val easy = preview.getValue(ReviewGrade.EASY)

        assertTrue("EASY($easy) >= GOOD($good)", easy >= good)
        assertTrue("GOOD($good) >= HARD($hard)", good >= hard)
        assertTrue("HARD($hard) >= AGAIN($again)", hard >= again)

        // Strict ordering holds on stability regardless of interval ties.
        val sAgain = FsrsScheduler.schedule(card, ReviewGrade.AGAIN, flashcardId, clock, params).stability
        val sHard = FsrsScheduler.schedule(card, ReviewGrade.HARD, flashcardId, clock, params).stability
        val sGood = FsrsScheduler.schedule(card, ReviewGrade.GOOD, flashcardId, clock, params).stability
        val sEasy = FsrsScheduler.schedule(card, ReviewGrade.EASY, flashcardId, clock, params).stability

        assertTrue("EASY stability($sEasy) > GOOD($sGood)", sEasy > sGood)
        assertTrue("GOOD stability($sGood) > HARD($sHard)", sGood > sHard)
        assertTrue("HARD stability($sHard) > AGAIN($sAgain)", sHard > sAgain)
    }

    @Test
    fun `previewAll for review card GOOD interval matches actual scheduled interval`() {
        val card = reviewCard(stability = 10.0, difficulty = 5.0)

        val preview = PreviewNextInterval.previewAll(card, clock, params)
        val actualScheduled = FsrsScheduler.schedule(card, ReviewGrade.GOOD, flashcardId, clock, params).interval

        assertEquals(actualScheduled, preview.getValue(ReviewGrade.GOOD))
    }

    @Test
    fun `previewAll does not mutate the original card`() {
        val card = reviewCard(stability = 10.0, difficulty = 5.0)
        val snapshotState = card.state
        val snapshotStability = card.stability

        PreviewNextInterval.previewAll(card, clock, params)

        assertEquals(snapshotState, card.state)
        assertEquals(snapshotStability, card.stability, 0.0)
    }

    @Test
    fun `previewAll all four grades present for a mature review card`() {
        val card = reviewCard(stability = 20.0, difficulty = 4.0)

        val preview = PreviewNextInterval.previewAll(card, clock, params)

        ReviewGrade.entries.forEach { grade ->
            assertNotNull("Missing preview for $grade", preview[grade])
        }
    }

    private fun reviewCard(stability: Double, difficulty: Double) = FsrsCard(
        flashcardId = flashcardId,
        state = FsrsState.REVIEW,
        stability = stability,
        difficulty = difficulty,
        lastReviewedAt = now.toEpochMilli() - 10 * MILLIS_PER_DAY,
        nextReviewAt = now.toEpochMilli(),
        interval = 10L,
        reps = 3L,
        lapses = 0L,
    )

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}

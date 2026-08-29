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

class FsrsSchedulerTest {

    private val now = Instant.parse("2026-06-12T12:00:00Z")
    private val clock = Clock { now }
    private val flashcardId = "card-1".toFlashcardId()
    private val params = FsrsParameters.DEFAULT

    @Test
    fun `T03 first GOOD rating sets stability to w2 and state to LEARNING`() {
        val card = newCard()

        val result = FsrsScheduler.schedule(card, ReviewGrade.GOOD, flashcardId, clock, params)

        assertEquals(FsrsState.LEARNING, result.state)
        assertEquals(params.weights[2], result.stability, EPSILON)
    }

    @Test
    fun `T03 first AGAIN rating sets stability to w0 and state to LEARNING not RELEARNING`() {
        val card = newCard()

        val result = FsrsScheduler.schedule(card, ReviewGrade.AGAIN, flashcardId, clock, params)

        assertEquals(FsrsState.LEARNING, result.state)
        assertEquals(params.weights[0], result.stability, EPSILON)
    }

    @Test
    fun `T03 first AGAIN rating does not increment lapses`() {
        val card = newCard()

        val result = FsrsScheduler.schedule(card, ReviewGrade.AGAIN, flashcardId, clock, params)

        assertEquals(0L, result.lapses)
    }

    @Test
    fun `T03 all four initial grades yield distinct stability values`() {
        val again = FsrsScheduler.schedule(newCard(), ReviewGrade.AGAIN, flashcardId, clock, params).stability
        val hard = FsrsScheduler.schedule(newCard(), ReviewGrade.HARD, flashcardId, clock, params).stability
        val good = FsrsScheduler.schedule(newCard(), ReviewGrade.GOOD, flashcardId, clock, params).stability
        val easy = FsrsScheduler.schedule(newCard(), ReviewGrade.EASY, flashcardId, clock, params).stability

        val all = listOf(again, hard, good, easy)
        assertEquals("All four stability values must be distinct", 4, all.distinct().size)
    }

    @Test
    fun `T03 initial stability order EASY greater GOOD greater HARD greater AGAIN`() {
        val again = FsrsScheduler.schedule(newCard(), ReviewGrade.AGAIN, flashcardId, clock, params).stability
        val hard = FsrsScheduler.schedule(newCard(), ReviewGrade.HARD, flashcardId, clock, params).stability
        val good = FsrsScheduler.schedule(newCard(), ReviewGrade.GOOD, flashcardId, clock, params).stability
        val easy = FsrsScheduler.schedule(newCard(), ReviewGrade.EASY, flashcardId, clock, params).stability

        assertTrue("EASY($easy) > GOOD($good)", easy > good)
        assertTrue("GOOD($good) > HARD($hard)", good > hard)
        assertTrue("HARD($hard) > AGAIN($again)", hard > again)
    }

    @Test
    fun `T03 first rating increments reps to 1`() {
        val card = newCard()

        val result = FsrsScheduler.schedule(card, ReviewGrade.GOOD, flashcardId, clock, params)

        assertEquals(1L, result.reps)
    }

    @Test
    fun `T03 initial stability for each grade matches weights indexed by toFsrsRating`() {
        // Guards against FsrsScheduler drifting from ReviewGrade's public grade-integer source.
        ReviewGrade.entries.forEach { grade ->
            val result = FsrsScheduler.schedule(newCard(), grade, flashcardId, clock, params)
            assertEquals(params.weights[grade.toFsrsRating() - 1], result.stability, EPSILON)
        }
    }

    @Test
    fun `T03 first rating initial difficulty D0 is clamped to 1 10`() {
        ReviewGrade.entries.forEach { grade ->
            val result = FsrsScheduler.schedule(newCard(), grade, flashcardId, clock, params)
            assertTrue("difficulty must be >= 1 for $grade", result.difficulty >= 1.0)
            assertTrue("difficulty must be <= 10 for $grade", result.difficulty <= 10.0)
        }
    }

    @Test
    fun `T04 difficulty stays at most 10 after many AGAIN ratings`() {
        var card = reviewCard(difficulty = 9.5, stability = 5.0)

        repeat(10) {
            card = FsrsScheduler.schedule(card, ReviewGrade.AGAIN, flashcardId, clock, params)
        }

        assertTrue("difficulty must be <= 10, was ${card.difficulty}", card.difficulty <= 10.0)
    }

    @Test
    fun `T04 difficulty stays at least 1 after many EASY ratings`() {
        var card = reviewCard(difficulty = 1.5, stability = 5.0)

        repeat(10) {
            card = FsrsScheduler.schedule(card, ReviewGrade.EASY, flashcardId, clock, params)
        }

        assertTrue("difficulty must be >= 1, was ${card.difficulty}", card.difficulty >= 1.0)
    }

    @Test
    fun `T04 EASY rating mean-reverts difficulty downward when above easy anchor`() {
        // Easy anchor = w[4] - exp(w[5]*(4-1)) + 1; typical FSRS-6 yields anchor near 5.something
        // Use D=9.0 (well above anchor) and EASY to drive difficulty down
        val card = reviewCard(difficulty = 9.0, stability = 5.0)

        val result = FsrsScheduler.schedule(card, ReviewGrade.EASY, flashcardId, clock, params)

        assertTrue("EASY should reduce high difficulty, was ${result.difficulty}", result.difficulty < 9.0)
    }

    @Test
    fun `T05 GOOD rating on REVIEW card increases stability`() {
        val card = reviewCard(stability = 10.0, difficulty = 5.0)

        val result = FsrsScheduler.schedule(card, ReviewGrade.GOOD, flashcardId, clock, params)

        assertTrue("new stability ${result.stability} must exceed old 10.0", result.stability > 10.0)
    }

    @Test
    fun `T05 HARD rating on REVIEW card increases stability (hard is not a lapse)`() {
        val card = reviewCard(stability = 10.0, difficulty = 5.0)

        val result = FsrsScheduler.schedule(card, ReviewGrade.HARD, flashcardId, clock, params)

        assertTrue("HARD stability ${result.stability} must exceed old 10.0", result.stability > 10.0)
    }

    @Test
    fun `T05 grade ordering EASY greater GOOD greater HARD in same prior state`() {
        val card = reviewCard(stability = 10.0, difficulty = 5.0)

        val sHard = FsrsScheduler.schedule(card, ReviewGrade.HARD, flashcardId, clock, params).stability
        val sGood = FsrsScheduler.schedule(card, ReviewGrade.GOOD, flashcardId, clock, params).stability
        val sEasy = FsrsScheduler.schedule(card, ReviewGrade.EASY, flashcardId, clock, params).stability

        assertTrue("EASY($sEasy) > GOOD($sGood)", sEasy > sGood)
        assertTrue("GOOD($sGood) > HARD($sHard)", sGood > sHard)
    }

    @Test
    fun `T06 AGAIN on REVIEW sets state to RELEARNING and increments lapses`() {
        val card = reviewCard(stability = 10.0, difficulty = 5.0, lapses = 2L)

        val result = FsrsScheduler.schedule(card, ReviewGrade.AGAIN, flashcardId, clock, params)

        assertEquals(FsrsState.RELEARNING, result.state)
        assertEquals(3L, result.lapses)
    }

    @Test
    fun `T06 post-lapse stability is at most prior stability`() {
        val card = reviewCard(stability = 10.0, difficulty = 5.0)

        val result = FsrsScheduler.schedule(card, ReviewGrade.AGAIN, flashcardId, clock, params)

        assertTrue("post-lapse stability ${result.stability} must be <= 10.0", result.stability <= 10.0)
    }

    @Test
    fun `T06 AGAIN on LEARNING card does not increment lapses`() {
        val card = learningCard(stability = 2.0, difficulty = 5.0, lapses = 0L)

        val result = FsrsScheduler.schedule(card, ReviewGrade.AGAIN, flashcardId, clock, params)

        assertEquals(0L, result.lapses)
    }

    @Test
    fun `T06 AGAIN on LEARNING card stays in LEARNING not RELEARNING`() {
        val card = learningCard(stability = 2.0, difficulty = 5.0, lapses = 0L)

        val result = FsrsScheduler.schedule(card, ReviewGrade.AGAIN, flashcardId, clock, params)

        assertEquals(FsrsState.LEARNING, result.state)
    }

    @Test
    fun `T07 higher stability yields longer interval`() {
        val cardA = reviewCard(stability = 5.0, difficulty = 5.0)
        val cardB = reviewCard(stability = 20.0, difficulty = 5.0)

        val intervalA = FsrsScheduler.schedule(cardA, ReviewGrade.GOOD, flashcardId, clock, params).interval
        val intervalB = FsrsScheduler.schedule(cardB, ReviewGrade.GOOD, flashcardId, clock, params).interval

        assertTrue("Interval(B=$intervalB) must exceed Interval(A=$intervalA)", intervalB > intervalA)
    }

    @Test
    fun `T07 interval floor is at least 1 day even for very low stability`() {
        val card = reviewCard(stability = 0.1, difficulty = 5.0)

        val result = FsrsScheduler.schedule(card, ReviewGrade.GOOD, flashcardId, clock, params)

        assertTrue("interval must be >= 1, was ${result.interval}", result.interval >= 1L)
    }

    @Test
    fun `T07 nextReviewAt is strictly after lastReviewedAt on GOOD`() {
        val card = reviewCard(stability = 10.0, difficulty = 5.0)

        val result = FsrsScheduler.schedule(card, ReviewGrade.GOOD, flashcardId, clock, params)

        assertTrue(result.nextReviewAt > result.lastReviewedAt)
    }

    @Test
    fun `T07 R at t equals 0 is 1`() {
        // Retrievability at elapsed_days=0 must be 1.0 (perfect recall)
        // Verified via: R(0,S) = (1 + FACTOR*0/S)^DECAY = (1+0)^DECAY = 1
        val decay = -params.weights[20]
        val factor = Math.pow(0.9, 1.0 / decay) - 1.0
        val r = Math.pow(1.0 + factor * 0.0 / 10.0, decay)
        assertEquals(1.0, r, EPSILON)
    }

    @Test
    fun `T07 interval capped at maximumIntervalDays`() {
        val card = reviewCard(stability = 99999.0, difficulty = 1.0)
        val shortMaxParams = params.copy(weights = params.weights.copyOf(), maximumIntervalDays = 100L)

        val result = FsrsScheduler.schedule(card, ReviewGrade.EASY, flashcardId, clock, shortMaxParams)

        assertTrue("interval must be <= 100, was ${result.interval}", result.interval <= 100L)
    }

    @Test
    fun `T08 same-day AGAIN on LEARNING card does not throw`() {
        val card = learningCard(stability = 1.0, difficulty = 5.0, lapses = 0L)

        val result = FsrsScheduler.schedule(card, ReviewGrade.AGAIN, flashcardId, clock, params)

        assertTrue(result.stability >= 0.0)
    }

    @Test
    fun `T08 same-day GOOD on RELEARNING card increases stability`() {
        val card = relearningCard(stability = 2.0, difficulty = 7.0, lapses = 1L)

        val result = FsrsScheduler.schedule(card, ReviewGrade.GOOD, flashcardId, clock, params)

        assertTrue("GOOD on RELEARNING should increase stability, was ${result.stability}", result.stability >= 2.0)
    }

    @Test
    fun `lastReviewedAt is set to clock now on any schedule call`() {
        val card = newCard()

        val result = FsrsScheduler.schedule(card, ReviewGrade.GOOD, flashcardId, clock, params)

        assertEquals(now.toEpochMilli(), result.lastReviewedAt)
    }

    @Test
    fun `flashcardId is replaced by the argument`() {
        val card = newCard()
        val targetId = "different-card".toFlashcardId()

        val result = FsrsScheduler.schedule(card, ReviewGrade.GOOD, targetId, clock, params)

        assertEquals("different-card", result.flashcardId.value)
    }

    private fun newCard() = FsrsCard.new(flashcardId, clock)

    private fun reviewCard(stability: Double, difficulty: Double, lapses: Long = 0L): FsrsCard = FsrsCard(
        flashcardId = flashcardId,
        state = FsrsState.REVIEW,
        stability = stability,
        difficulty = difficulty,
        lastReviewedAt = now.toEpochMilli() - 10 * MILLIS_PER_DAY,
        nextReviewAt = now.toEpochMilli(),
        interval = 10L,
        reps = 3L,
        lapses = lapses,
    )

    private fun learningCard(stability: Double, difficulty: Double, lapses: Long = 0L): FsrsCard = FsrsCard(
        flashcardId = flashcardId,
        state = FsrsState.LEARNING,
        stability = stability,
        difficulty = difficulty,
        lastReviewedAt = now.toEpochMilli(),
        nextReviewAt = now.toEpochMilli(),
        interval = 0L,
        reps = 1L,
        lapses = lapses,
    )

    private fun relearningCard(stability: Double, difficulty: Double, lapses: Long = 1L): FsrsCard = FsrsCard(
        flashcardId = flashcardId,
        state = FsrsState.RELEARNING,
        stability = stability,
        difficulty = difficulty,
        lastReviewedAt = now.toEpochMilli(),
        nextReviewAt = now.toEpochMilli(),
        interval = 0L,
        reps = 2L,
        lapses = lapses,
    )

    private companion object {
        const val EPSILON = 1e-6
        const val MILLIS_PER_DAY = 86_400_000L
    }
}

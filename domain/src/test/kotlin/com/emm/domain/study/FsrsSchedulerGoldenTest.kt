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

/**
 * T-09: Golden-value parity tests.
 *
 * These tests assert INTERNAL consistency and behavioral ordering of the FSRS-6
 * implementation against the canonical default weight vector.
 *
 * TODO(verify): pin exact stability/difficulty/interval values against the upstream
 * FSRS-6 reference implementation (open-spaced-repetition/py-fsrs) before finalizing PR.
 * Current assertions verify ordering, monotonicity, and behavioral contracts from the spec
 * but do NOT pin specific floating-point values to the canonical reference output, because
 * those values cannot be verified offline without running the reference implementation.
 *
 * The weight vector itself (FsrsParameters.FSRS6_DEFAULT_WEIGHTS) must be verified
 * against the canonical source separately.
 */
class FsrsSchedulerGoldenTest {

    private val params = FsrsParameters.DEFAULT
    private val flashcardId = "card-golden".toFlashcardId()

    // Starting epoch for deterministic elapsed-days computation
    private val epoch = Instant.parse("2026-01-01T00:00:00Z")

    // ----------------------------------------------------------------
    // GOOD x3 sequence
    // ----------------------------------------------------------------

    @Test
    fun `golden GOOD sequence stability increases from REVIEW state`() {
        // Short-term path (LEARNING) may produce s1==s0 when the GOOD increase hits the 1.0
        // floor — that is correct canonical behavior. Strict increase is guaranteed once the
        // card enters REVIEW state and the recall-stability formula applies.
        val steps = goodGoodGoodSteps()
        val reviewSteps = steps.filter { it.state == FsrsState.REVIEW }

        assertTrue("Expected at least 2 REVIEW-state steps", reviewSteps.size >= 2)
        assertTrue("s0(${steps[0].stability}) > 0", steps[0].stability > 0.0)
        for (i in 1 until reviewSteps.size) {
            val prev = reviewSteps[i - 1].stability
            val curr = reviewSteps[i].stability
            assertTrue("stability[$i]=$curr must exceed stability[${i - 1}]=$prev", curr > prev)
        }
    }

    @Test
    fun `golden GOOD sequence difficulty stays in bounds after 3 GOOD`() {
        val steps = goodGoodGoodSteps()
        steps.forEach { card ->
            assertTrue("difficulty must be >= 1, was ${card.difficulty}", card.difficulty >= 1.0)
            assertTrue("difficulty must be <= 10, was ${card.difficulty}", card.difficulty <= 10.0)
        }
    }

    @Test
    fun `golden GOOD sequence intervals non-decreasing from first REVIEW state`() {
        // The NEW->LEARNING short-term transition (step-0 to step-1) is not guaranteed
        // strictly monotonic at integer resolution. Monotonicity is asserted from the
        // first REVIEW-state card (step-1) onward.
        val steps = goodGoodGoodSteps()
        val reviewSteps = steps.filter { it.state == FsrsState.REVIEW }
        assertTrue("Expected at least 2 REVIEW-state steps", reviewSteps.size >= 2)
        for (i in 1 until reviewSteps.size) {
            val prevInterval = reviewSteps[i - 1].interval
            val currInterval = reviewSteps[i].interval
            assertTrue(
                "interval[$i]=$currInterval must be >= interval[${i - 1}]=$prevInterval",
                currInterval >= prevInterval,
            )
        }
        // Stability must be strictly increasing across REVIEW-state steps.
        // The short-term path (LEARNING) may produce s1==s0 at the 1.0 floor — that is correct.
        for (i in 1 until reviewSteps.size) {
            val prevS = reviewSteps[i - 1].stability
            val currS = reviewSteps[i].stability
            assertTrue("stability[$i]=$currS must exceed stability[${i - 1}]=$prevS", currS > prevS)
        }
    }

    @Test
    fun `golden GOOD sequence state after first GOOD is LEARNING`() {
        val steps = goodGoodGoodSteps()
        assertEquals(FsrsState.LEARNING, steps[0].state)
    }

    @Test
    fun `golden GOOD sequence state after second GOOD is REVIEW`() {
        val steps = goodGoodGoodSteps()
        assertEquals(FsrsState.REVIEW, steps[1].state)
    }

    @Test
    fun `golden GOOD sequence state after third GOOD is REVIEW`() {
        val steps = goodGoodGoodSteps()
        assertEquals(FsrsState.REVIEW, steps[2].state)
    }

    @Test
    fun `golden GOOD sequence initial stability matches w2 exactly`() {
        val steps = goodGoodGoodSteps()
        assertEquals(params.weights[2], steps[0].stability, 1e-9)
    }

    // ----------------------------------------------------------------
    // Lapse-then-recovery sequence: GOOD GOOD AGAIN GOOD
    // ----------------------------------------------------------------

    @Test
    fun `golden lapse sequence post-lapse stability is at most pre-lapse stability`() {
        val steps = lapseThenRecoverySteps()
        val preReview = steps.preReview
        val postLapse = steps.postLapse

        assertTrue(
            "post-lapse stability must be <= pre-lapse ${preReview.stability}",
            postLapse.stability <= preReview.stability,
        )
    }

    @Test
    fun `golden lapse sequence lapses counter incremented to 1 after AGAIN on REVIEW`() {
        val postLapse = lapseThenRecoverySteps().postLapse
        assertEquals(1L, postLapse.lapses)
    }

    @Test
    fun `golden lapse sequence state is RELEARNING after AGAIN on REVIEW`() {
        val postLapse = lapseThenRecoverySteps().postLapse
        assertEquals(FsrsState.RELEARNING, postLapse.state)
    }

    @Test
    fun `golden lapse recovery GOOD after AGAIN produces non-negative stability`() {
        val postRecovery = lapseThenRecoverySteps().postRecovery
        assertTrue("post-recovery stability must be >= 0", postRecovery.stability >= 0.0)
    }

    // ----------------------------------------------------------------
    // Internal consistency: behavioral ordering from spec
    // ----------------------------------------------------------------

    @Test
    fun `golden behavioral EASY greater GOOD greater HARD stability ordering on same REVIEW card`() {
        val card = reviewCardAtDay(10)
        val sHard = schedule(card, ReviewGrade.HARD, 10).stability
        val sGood = schedule(card, ReviewGrade.GOOD, 10).stability
        val sEasy = schedule(card, ReviewGrade.EASY, 10).stability

        assertTrue("EASY($sEasy) > GOOD($sGood)", sEasy > sGood)
        assertTrue("GOOD($sGood) > HARD($sHard)", sGood > sHard)
    }

    @Test
    fun `golden behavioral higher S yields longer I at same retention`() {
        val cardLowS = reviewCardWithStability(5.0, 10)
        val cardHighS = reviewCardWithStability(20.0, 10)

        val iLow = schedule(cardLowS, ReviewGrade.GOOD, 10).interval
        val iHigh = schedule(cardHighS, ReviewGrade.GOOD, 10).interval

        assertTrue("higher S must yield longer I: $iHigh > $iLow", iHigh > iLow)
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private fun goodGoodGoodSteps(): List<FsrsCard> {
        var card = FsrsCard.new(flashcardId, clockAt(epoch))
        val results = mutableListOf<FsrsCard>()
        card = schedule(card, ReviewGrade.GOOD, 0)
        results += card
        card = schedule(card, ReviewGrade.GOOD, 1)
        results += card
        card = schedule(card, ReviewGrade.GOOD, card.interval.toInt())
        results += card
        return results
    }

    private data class LapseSteps(
        val initial: FsrsCard,
        val preReview: FsrsCard,
        val postLapse: FsrsCard,
        val postRecovery: FsrsCard,
    )

    private fun lapseThenRecoverySteps(): LapseSteps {
        val initial = FsrsCard.new(flashcardId, clockAt(epoch))
        var card = initial
        card = schedule(card, ReviewGrade.GOOD, 0)
        card = schedule(card, ReviewGrade.GOOD, 1)
        val preReview = card
        card = schedule(card, ReviewGrade.AGAIN, card.interval.toInt())
        val postLapse = card
        card = schedule(card, ReviewGrade.GOOD, 0)
        return LapseSteps(initial, preReview, postLapse, card)
    }

    private fun schedule(card: FsrsCard, grade: ReviewGrade, elapsedDaysFromEpoch: Int): FsrsCard {
        val reviewInstant = epoch.plusSeconds(elapsedDaysFromEpoch.toLong() * SECONDS_PER_DAY)
        return FsrsScheduler.schedule(card, grade, flashcardId, clockAt(reviewInstant), params)
    }

    private fun reviewCardAtDay(elapsedDays: Int) = FsrsCard(
        flashcardId = flashcardId,
        state = FsrsState.REVIEW,
        stability = 10.0,
        difficulty = 5.0,
        lastReviewedAt = epoch.toEpochMilli(),
        nextReviewAt = epoch.plusSeconds(elapsedDays.toLong() * SECONDS_PER_DAY).toEpochMilli(),
        interval = elapsedDays.toLong(),
        reps = 3L,
        lapses = 0L,
    )

    private fun reviewCardWithStability(stability: Double, elapsedDays: Int) = FsrsCard(
        flashcardId = flashcardId,
        state = FsrsState.REVIEW,
        stability = stability,
        difficulty = 5.0,
        lastReviewedAt = epoch.toEpochMilli(),
        nextReviewAt = epoch.plusSeconds(elapsedDays.toLong() * SECONDS_PER_DAY).toEpochMilli(),
        interval = elapsedDays.toLong(),
        reps = 3L,
        lapses = 0L,
    )

    private fun clockAt(instant: Instant) = Clock { instant }

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
    }
}

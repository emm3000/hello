package com.emm.domain.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsParameters
import com.emm.domain.flashcard.FsrsState
import com.emm.domain.ids.FlashcardId
import com.emm.domain.time.Clock
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Pure-JVM FSRS-6 scheduler.
 *
 * Equations follow the FSRS-6 specification from open-spaced-repetition.
 * All math is in Double. No Android dependencies.
 *
 * Grade integers (per FSRS convention): AGAIN=1, HARD=2, GOOD=3, EASY=4.
 */
@Suppress("MagicNumber")
object FsrsScheduler {

    /**
     * Schedule a card given the user's grade.
     *
     * Dispatches to:
     *  - initial path  (state == NEW)
     *  - short-term path (state == LEARNING or RELEARNING)
     *  - long-term path (state == REVIEW)
     */
    fun schedule(
        card: FsrsCard,
        grade: ReviewGrade,
        flashcardId: FlashcardId,
        clock: Clock,
        params: FsrsParameters = FsrsParameters.DEFAULT,
    ): FsrsCard {
        val w = params.weights
        val gradeInt = grade.toFsrsInt()
        val now = clock.now()
        val nowMs = now.toEpochMilli()
        val elapsedDays = elapsedDays(card.lastReviewedAt, nowMs)

        return when (card.state) {
            FsrsState.NEW -> scheduleNew(
                card = card,
                gradeInt = gradeInt,
                w = w,
                params = params,
                flashcardId = flashcardId,
                nowMs = nowMs,
            )
            FsrsState.LEARNING, FsrsState.RELEARNING -> scheduleShortTerm(
                card = card,
                gradeInt = gradeInt,
                w = w,
                params = params,
                flashcardId = flashcardId,
                nowMs = nowMs,
            )
            FsrsState.REVIEW -> scheduleLongTerm(
                card = card,
                gradeInt = gradeInt,
                w = w,
                params = params,
                flashcardId = flashcardId,
                nowMs = nowMs,
                elapsedDays = elapsedDays,
            )
        }
    }

    // ----------------------------------------------------------------
    // Initial path (NEW card, first rating)
    // ----------------------------------------------------------------

    private fun scheduleNew(
        card: FsrsCard,
        gradeInt: Int,
        w: DoubleArray,
        params: FsrsParameters,
        flashcardId: FlashcardId,
        nowMs: Long,
    ): FsrsCard {
        val s0 = initialStability(gradeInt, w)
        val d0 = initialDifficulty(gradeInt, w) // clamped to [1,10] by default
        val interval = computeInterval(s0, params)
        val nextMs = nowMs + interval * MILLIS_PER_DAY
        return card.copy(
            flashcardId = flashcardId,
            state = FsrsState.LEARNING,
            stability = s0,
            difficulty = d0,
            lastReviewedAt = nowMs,
            nextReviewAt = nextMs,
            interval = interval,
            reps = card.reps + 1L,
            lapses = card.lapses, // first exposure: no lapse
        )
    }

    // ----------------------------------------------------------------
    // Short-term path (LEARNING or RELEARNING — same-day reviews)
    // Uses w[17], w[18], w[19].
    // ----------------------------------------------------------------

    private fun scheduleShortTerm(
        card: FsrsCard,
        gradeInt: Int,
        w: DoubleArray,
        params: FsrsParameters,
        flashcardId: FlashcardId,
        nowMs: Long,
    ): FsrsCard {
        val d = updateDifficulty(card.difficulty, gradeInt, w)
        val sNew = shortTermStability(card.stability, gradeInt, w)

        // Lapses: only AGAIN on REVIEW increments lapses; LEARNING/RELEARNING AGAIN does not
        val newLapses = card.lapses

        val newState = if (gradeInt == GRADE_AGAIN) {
            card.state // stays in LEARNING or RELEARNING
        } else {
            FsrsState.REVIEW // graduate on HARD/GOOD/EASY
        }

        val interval = if (newState == FsrsState.REVIEW) computeInterval(sNew, params) else 0L
        val nextMs = if (newState == FsrsState.REVIEW) {
            nowMs + interval * MILLIS_PER_DAY
        } else {
            nowMs + TEN_MINUTES_MS // same-day minimal next
        }

        return card.copy(
            flashcardId = flashcardId,
            state = newState,
            stability = sNew,
            difficulty = d,
            lastReviewedAt = nowMs,
            nextReviewAt = nextMs,
            interval = interval,
            reps = card.reps + 1L,
            lapses = newLapses,
        )
    }

    // ----------------------------------------------------------------
    // Long-term path (REVIEW card)
    // ----------------------------------------------------------------

    private fun scheduleLongTerm(
        card: FsrsCard,
        gradeInt: Int,
        w: DoubleArray,
        params: FsrsParameters,
        flashcardId: FlashcardId,
        nowMs: Long,
        elapsedDays: Double,
    ): FsrsCard {
        // Stability is computed from the card's CURRENT (pre-update) difficulty.
        // Difficulty is updated separately afterwards.
        val r = retrievability(elapsedDays, card.stability, w)
        val currentD = card.difficulty

        val (newStability, newState, newLapses) = if (gradeInt == GRADE_AGAIN) {
            val sLapse = postLapseStability(card.stability, currentD, r, w)
            Triple(sLapse, FsrsState.RELEARNING, card.lapses + 1L)
        } else {
            val sSuccess = successStability(card.stability, currentD, r, gradeInt, w)
            Triple(sSuccess, FsrsState.REVIEW, card.lapses)
        }

        val d = updateDifficulty(card.difficulty, gradeInt, w)

        val interval = computeInterval(newStability, params)
        val nextMs = nowMs + interval * MILLIS_PER_DAY

        return card.copy(
            flashcardId = flashcardId,
            state = newState,
            stability = newStability,
            difficulty = d,
            lastReviewedAt = nowMs,
            nextReviewAt = nextMs,
            interval = interval,
            reps = card.reps + 1L,
            lapses = newLapses,
        )
    }

    // ----------------------------------------------------------------
    // FSRS-6 core equations
    // ----------------------------------------------------------------

    /** S0(G) = clampStability(w[G-1]) */
    internal fun initialStability(gradeInt: Int, w: DoubleArray): Double =
        clampStability(w[gradeInt - 1])

    /**
     * D0(G) = w[4] - exp(w[5] * (G - 1)) + 1
     * Clamped to [1, 10] only when clamp=true.
     */
    internal fun initialDifficulty(gradeInt: Int, w: DoubleArray, clamp: Boolean = true): Double {
        val raw = w[4] - exp(w[5] * (gradeInt - 1)) + 1.0
        return if (clamp) clampDifficulty(raw) else raw
    }

    /**
     * Difficulty update (_next_difficulty):
     *   arg1 = D0(EASY=4, clamp=false)   // unclamped initial difficulty for Easy
     *   delta = -(w[6] * (grade - 3))
     *   linearDamping = (10.0 - difficulty) * delta / 9.0
     *   arg2 = difficulty + linearDamping
     *   nextD = w[7]*arg1 + (1 - w[7])*arg2
     *   return clampDifficulty(nextD)
     */
    internal fun updateDifficulty(d: Double, gradeInt: Int, w: DoubleArray): Double {
        val arg1 = initialDifficulty(GRADE_EASY, w, clamp = false)
        val delta = -(w[6] * (gradeInt - 3))
        val linearDamping = (10.0 - d) * delta / 9.0
        val arg2 = d + linearDamping
        return clampDifficulty(w[7] * arg1 + (1.0 - w[7]) * arg2)
    }

    /** Forgetting curve: R(t, S) = (1 + FACTOR * t / S) ^ DECAY */
    internal fun retrievability(elapsedDays: Double, stability: Double, w: DoubleArray): Double {
        if (stability <= 0.0) return 0.0
        val decay = -w[20]
        val factor = 0.9.pow(1.0 / decay) - 1.0
        return (1.0 + factor * elapsedDays / stability).pow(decay)
    }

    /**
     * Recall stability (HARD/GOOD/EASY on REVIEW) — _next_recall_stability:
     * hardPenalty = w[15] if HARD else 1.0
     * easyBonus   = w[16] if EASY else 1.0
     * S' = S * (1 + exp(w[8]) * (11 - D) * S^(-w[9]) * (exp((1-R)*w[10]) - 1) * hardPenalty * easyBonus)
     * return clampStability(S')
     */
    internal fun successStability(
        s: Double,
        d: Double,
        r: Double,
        gradeInt: Int,
        w: DoubleArray,
    ): Double {
        val hardPenalty = if (gradeInt == GRADE_HARD) w[15] else 1.0
        val easyBonus = if (gradeInt == GRADE_EASY) w[16] else 1.0
        val sPrime = s * (
            1.0 + exp(w[8]) * (11.0 - d) * s.pow(-w[9]) *
                (exp((1.0 - r) * w[10]) - 1.0) * hardPenalty * easyBonus
            )
        return clampStability(sPrime)
    }

    /**
     * Forget/lapse stability (AGAIN on REVIEW) — _next_forget_stability:
     * longTerm  = w[11] * D^(-w[12]) * ((S+1)^w[13] - 1) * exp((1-R)*w[14])
     * shortTerm = S / exp(w[17] * w[18])
     * S' = min(longTerm, shortTerm)
     * return clampStability(S')
     */
    internal fun postLapseStability(s: Double, d: Double, r: Double, w: DoubleArray): Double {
        val longTerm = w[11] * d.pow(-w[12]) * ((s + 1.0).pow(w[13]) - 1.0) * exp((1.0 - r) * w[14])
        val shortTerm = s / exp(w[17] * w[18])
        return clampStability(min(longTerm, shortTerm))
    }

    /**
     * Short-term stability (LEARNING/RELEARNING) — _short_term_stability:
     * increase = exp(w[17] * (grade - 3 + w[18])) * S^(-w[19])
     * if grade==GOOD || grade==EASY: increase = max(increase, 1.0)
     * S' = clampStability(S * increase)
     */
    internal fun shortTermStability(s: Double, gradeInt: Int, w: DoubleArray): Double {
        var increase = exp(w[17] * (gradeInt - 3 + w[18])) * s.pow(-w[19])
        if (gradeInt == GRADE_GOOD || gradeInt == GRADE_EASY) {
            increase = max(increase, 1.0)
        }
        return clampStability(s * increase)
    }

    /**
     * Interval from stability and target retention:
     *   I = (S / FACTOR) * (R_req^(1/DECAY) - 1)
     * Non-finite or non-positive S returns 1L.
     * Clamped to [1, maximumIntervalDays].
     */
    internal fun computeInterval(s: Double, params: FsrsParameters): Long {
        if (!s.isFinite() || s <= 0.0) return 1L
        val decay = -params.weights[20]
        val factor = 0.9.pow(1.0 / decay) - 1.0
        val rReq = params.requestedRetention
        val intervalDouble = (s / factor) * (rReq.pow(1.0 / decay) - 1.0)
        return intervalDouble.roundToLong().coerceIn(1L, params.maximumIntervalDays)
    }

    // ----------------------------------------------------------------
    // Utilities
    // ----------------------------------------------------------------

    private fun clampStability(s: Double): Double = max(s, STABILITY_MIN)

    private fun clampDifficulty(d: Double): Double = d.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)

    private fun elapsedDays(lastReviewedAtMs: Long, nowMs: Long): Double {
        val diffMs = (nowMs - lastReviewedAtMs).coerceAtLeast(0L)
        return diffMs.toDouble() / MILLIS_PER_DAY
    }

    private fun ReviewGrade.toFsrsInt(): Int = when (this) {
        ReviewGrade.AGAIN -> GRADE_AGAIN
        ReviewGrade.HARD -> GRADE_HARD
        ReviewGrade.GOOD -> GRADE_GOOD
        ReviewGrade.EASY -> GRADE_EASY
    }

    private const val GRADE_AGAIN = 1
    private const val GRADE_HARD = 2
    private const val GRADE_GOOD = 3
    private const val GRADE_EASY = 4

    private const val MIN_DIFFICULTY = 1.0
    private const val MAX_DIFFICULTY = 10.0
    private const val STABILITY_MIN = 0.001
    private const val MILLIS_PER_DAY = 86_400_000L
    private const val TEN_MINUTES_MS = 600_000L
}

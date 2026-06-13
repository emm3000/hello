package com.emm.domain.flashcard

/**
 * FSRS-6 scheduling parameters.
 *
 * weights: 21-element vector (w[0]..w[20]) from the published FSRS-6 default.
 * Source: open-spaced-repetition/py-fsrs fsrs/scheduler.py (DEFAULT_PARAMETERS + FSRS_DEFAULT_DECAY=0.1542).
 * Verified 2026-06.
 *
 * Roles per index:
 *   w[0..3]  — S0(AGAIN/HARD/GOOD/EASY): initial stability per first rating
 *   w[4]     — D0 base (initial difficulty anchor)
 *   w[5]     — D0 grade-decay exponent
 *   w[6]     — difficulty delta coefficient
 *   w[7]     — difficulty mean-reversion weight
 *   w[8]     — stability increase exponent
 *   w[9]     — stability decay coefficient
 *   w[10]    — retrievability exponent in stability update
 *   w[11]    — post-lapse stability base
 *   w[12]    — post-lapse difficulty exponent
 *   w[13]    — post-lapse stability exponent
 *   w[14]    — post-lapse retrievability exponent
 *   w[15]    — HARD penalty
 *   w[16]    — EASY bonus
 *   w[17..19]— short-term path (LEARNING/RELEARNING same-day)
 *   w[20]    — forgetting-curve decay magnitude (DECAY = -w[20])
 */
data class FsrsParameters(
    val weights: DoubleArray,
    val requestedRetention: Double,
    val maximumIntervalDays: Long = 36500L,
) {

    init {
        require(weights.size == WEIGHT_COUNT) {
            "FSRS-6 requires exactly $WEIGHT_COUNT weights, got ${weights.size}."
        }
        require(requestedRetention in MIN_RETENTION..MAX_RETENTION) {
            "requestedRetention must be in [$MIN_RETENTION, $MAX_RETENTION], was $requestedRetention."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FsrsParameters) return false
        return weights.contentEquals(other.weights) &&
            requestedRetention == other.requestedRetention &&
            maximumIntervalDays == other.maximumIntervalDays
    }

    override fun hashCode(): Int {
        var result = weights.contentHashCode()
        result = 31 * result + requestedRetention.hashCode()
        result = 31 * result + maximumIntervalDays.hashCode()
        return result
    }

    companion object {

        private const val WEIGHT_COUNT = 21
        private const val MIN_RETENTION = 0.01
        private const val MAX_RETENTION = 0.99

        /**
         * Canonical FSRS-6 defaults from open-spaced-repetition/py-fsrs fsrs/scheduler.py
         * (DEFAULT_PARAMETERS + FSRS_DEFAULT_DECAY=0.1542). Verified 2026-06.
         */
        @Suppress("MagicNumber")
        val FSRS6_DEFAULT_WEIGHTS = doubleArrayOf(
            0.212, // w[0]  S0(AGAIN)
            1.2931, // w[1]  S0(HARD)
            2.3065, // w[2]  S0(GOOD)
            8.2956, // w[3]  S0(EASY)
            6.4133, // w[4]  D0 base
            0.8334, // w[5]  D0 grade-decay exponent
            3.0194, // w[6]  difficulty delta coefficient
            0.001, // w[7]  difficulty mean-reversion weight
            1.8722, // w[8]  stability-increase exponent
            0.1666, // w[9]  stability decay coefficient
            0.796, // w[10] retrievability exponent in stability update
            1.4835, // w[11] post-lapse stability base
            0.0614, // w[12] post-lapse difficulty exponent
            0.2629, // w[13] post-lapse stability exponent
            1.6483, // w[14] post-lapse retrievability exponent
            0.6014, // w[15] HARD penalty
            1.8729, // w[16] EASY bonus
            0.5425, // w[17] short-term stability increase
            0.0912, // w[18] short-term grade multiplier
            0.0658, // w[19] short-term S^(-w19) exponent
            0.1542, // w[20] forgetting-curve DECAY magnitude (FSRS_DEFAULT_DECAY)
        )

        val DEFAULT = FsrsParameters(
            weights = FSRS6_DEFAULT_WEIGHTS.copyOf(),
            requestedRetention = 0.90,
            maximumIntervalDays = 36500L,
        )
    }
}

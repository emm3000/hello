package com.emm.domain.generation

import java.time.Instant

interface GenerationQuota {

    suspend fun tryConsume(): Outcome

    fun remainingToday(): Int

    sealed interface Outcome {
        data object Allowed : Outcome
        data class Exceeded(val limit: Int, val resetAt: Instant) : Outcome
    }

    object AlwaysAllow : GenerationQuota {
        override suspend fun tryConsume(): Outcome = Outcome.Allowed
        override fun remainingToday(): Int = Int.MAX_VALUE
    }
}

class GenerationQuotaExceededException(
    val limit: Int,
    val resetAt: Instant,
) : RuntimeException("Daily generation quota of $limit reached; resets at $resetAt")

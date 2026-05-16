package com.emm.domain.generation

import java.time.Instant

interface GenerationQuota {

    suspend fun tryConsume(): Outcome

    sealed interface Outcome {
        data object Allowed : Outcome
        data class Exceeded(val limit: Int, val resetAt: Instant) : Outcome
    }

    object AlwaysAllow : GenerationQuota {
        override suspend fun tryConsume(): Outcome = Outcome.Allowed
    }
}

class GenerationQuotaExceededException(
    val limit: Int,
    val resetAt: Instant,
) : RuntimeException("Daily generation quota of $limit reached; resets at $resetAt")

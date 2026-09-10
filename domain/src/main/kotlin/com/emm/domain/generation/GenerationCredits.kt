package com.emm.domain.generation

import java.time.Instant
import java.time.temporal.ChronoUnit

data class GenerationCredits(
    val remaining: Int,
    val observedAt: Instant,
)

fun GenerationCredits.isFreshAt(now: Instant): Boolean {
    return observedAt.truncatedTo(ChronoUnit.DAYS) == now.truncatedTo(ChronoUnit.DAYS)
}

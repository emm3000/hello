package com.emm.domain.generation

import java.time.Instant

data class GenerationCredits(
    val remaining: Int,
    val resetAt: Instant,
)

fun GenerationCredits.isFreshAt(now: Instant): Boolean = now.isBefore(resetAt)

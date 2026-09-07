package com.emm.domain.generation

import java.time.Instant

class GenerationCreditsExhaustedException(
    val resetAt: Instant?,
    val reason: String? = null,
) : RuntimeException(reason ?: "Generation credits exhausted")

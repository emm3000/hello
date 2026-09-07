package com.emm.domain.generation

import java.time.Instant

class GenerationCreditsExhaustedException(
    val resetAt: Instant?,
) : RuntimeException("Generation credits exhausted")

package com.emm.hello.enrichment

import com.emm.domain.generation.AmbiguousGenerationInputException
import com.emm.domain.generation.GenerationCreditsExhaustedException

object EnrichmentFailureReason {

    fun of(error: Throwable): String? {
        return when (error) {
            is AmbiguousGenerationInputException -> error.reason
            is GenerationCreditsExhaustedException -> error.reason
            else -> null
        }
    }
}

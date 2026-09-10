package com.emm.hello.enrichment

import com.emm.domain.generation.AmbiguousGenerationInputException
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.generation.GenerationCreditsExhaustedException
import com.emm.domain.generation.GenerationRefusalCode

object EnrichmentFailures {

    fun of(error: Throwable): EnrichmentFailure? {
        return when (error) {
            is AmbiguousGenerationInputException -> EnrichmentFailure(error.code, error.reason)
            is GenerationCreditsExhaustedException ->
                EnrichmentFailure(GenerationRefusalCode.CreditsExhausted, error.reason)
            else -> null
        }
    }
}

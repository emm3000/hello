package com.emm.hello.enrichment

import com.emm.domain.generation.AmbiguousGenerationInputException
import com.emm.domain.generation.AppCheckRejectedException
import com.emm.domain.generation.GenerationCreditsExhaustedException
import com.emm.domain.validation.DomainValidationException

object EnrichmentRetryPolicy {

    fun shouldRetry(error: Throwable): Boolean {
        return when (error) {
            is DomainValidationException -> false
            is AmbiguousGenerationInputException -> false
            is AppCheckRejectedException -> false
            is GenerationCreditsExhaustedException -> false
            else -> true
        }
    }
}

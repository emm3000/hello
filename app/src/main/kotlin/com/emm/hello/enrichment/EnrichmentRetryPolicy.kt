package com.emm.hello.enrichment

import com.emm.domain.generation.GenerationQuotaExceededException
import com.emm.domain.validation.DomainValidationException

object EnrichmentRetryPolicy {

    fun shouldRetry(error: Throwable): Boolean {
        return when (error) {
            is DomainValidationException -> false
            is GenerationQuotaExceededException -> false
            else -> true
        }
    }
}

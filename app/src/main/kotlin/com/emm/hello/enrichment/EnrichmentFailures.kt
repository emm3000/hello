package com.emm.hello.enrichment

import com.emm.domain.generation.AmbiguousGenerationInputException
import com.emm.domain.generation.AppCheckRejectedException
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.generation.EnrichmentFailureCause
import com.emm.domain.generation.GenerationCreditsExhaustedException
import com.emm.domain.generation.InputProblem
import com.emm.domain.generation.toFailureCause

object EnrichmentFailures {

    fun of(error: Throwable): EnrichmentFailure {
        return when (error) {
            is AppCheckRejectedException -> EnrichmentFailure(EnrichmentFailureCause.AppCheckRejected, null)
            is AmbiguousGenerationInputException -> EnrichmentFailure(error.causeOrUnintelligible(), error.reason)
            is GenerationCreditsExhaustedException ->
                EnrichmentFailure(EnrichmentFailureCause.CreditsExhausted, error.reason)
            else -> EnrichmentFailure(EnrichmentFailureCause.Technical, null)
        }
    }

    private fun AmbiguousGenerationInputException.causeOrUnintelligible(): EnrichmentFailureCause =
        code?.toFailureCause() ?: EnrichmentFailureCause.WordProblem(InputProblem.Unintelligible)
}

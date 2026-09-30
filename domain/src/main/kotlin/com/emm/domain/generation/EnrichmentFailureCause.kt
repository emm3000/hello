package com.emm.domain.generation

import java.time.Instant

sealed interface EnrichmentFailureCause {

    data object Technical : EnrichmentFailureCause

    data object AppCheckRejected : EnrichmentFailureCause

    data class WordProblem(val problem: InputProblem) : EnrichmentFailureCause

    data object CreditsExhausted : EnrichmentFailureCause
}

enum class InputProblem {
    EmptyInput,
    Unintelligible,
    Contradictory,
    Unmappable,
}

fun EnrichmentFailureCause.canRetryAt(now: Instant, credits: GenerationCredits?): Boolean = when (this) {
    EnrichmentFailureCause.Technical -> true
    EnrichmentFailureCause.AppCheckRejected -> false
    is EnrichmentFailureCause.WordProblem -> false
    EnrichmentFailureCause.CreditsExhausted -> credits == null || !credits.isFreshAt(now)
}

fun GenerationRefusalCode.toFailureCause(): EnrichmentFailureCause = when (this) {
    GenerationRefusalCode.EmptyInput -> EnrichmentFailureCause.WordProblem(InputProblem.EmptyInput)
    GenerationRefusalCode.Unintelligible -> EnrichmentFailureCause.WordProblem(InputProblem.Unintelligible)
    GenerationRefusalCode.Contradictory -> EnrichmentFailureCause.WordProblem(InputProblem.Contradictory)
    GenerationRefusalCode.Unmappable -> EnrichmentFailureCause.WordProblem(InputProblem.Unmappable)
    GenerationRefusalCode.CreditsExhausted -> EnrichmentFailureCause.CreditsExhausted
}

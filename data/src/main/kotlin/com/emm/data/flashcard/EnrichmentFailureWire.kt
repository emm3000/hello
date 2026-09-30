package com.emm.data.flashcard

import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.generation.EnrichmentFailureCause
import com.emm.domain.generation.InputProblem

private const val TECHNICAL: String = "technical"
private const val APP_CHECK_REJECTED: String = "app_check_rejected"
private const val EMPTY_INPUT: String = "empty_input"
private const val UNINTELLIGIBLE: String = "unintelligible"
private const val CONTRADICTORY: String = "contradictory"
private const val UNMAPPABLE: String = "unmappable"
private const val CREDITS_EXHAUSTED: String = "credits_exhausted"

internal fun EnrichmentFailureCause.toWire(): String = when (this) {
    EnrichmentFailureCause.Technical -> TECHNICAL
    EnrichmentFailureCause.AppCheckRejected -> APP_CHECK_REJECTED
    is EnrichmentFailureCause.WordProblem -> problem.toWire()
    EnrichmentFailureCause.CreditsExhausted -> CREDITS_EXHAUSTED
}

private fun InputProblem.toWire(): String = when (this) {
    InputProblem.EmptyInput -> EMPTY_INPUT
    InputProblem.Unintelligible -> UNINTELLIGIBLE
    InputProblem.Contradictory -> CONTRADICTORY
    InputProblem.Unmappable -> UNMAPPABLE
}

internal fun enrichmentFailureCauseFromWire(raw: String?): EnrichmentFailureCause = when (raw) {
    APP_CHECK_REJECTED -> EnrichmentFailureCause.AppCheckRejected
    EMPTY_INPUT -> EnrichmentFailureCause.WordProblem(InputProblem.EmptyInput)
    UNINTELLIGIBLE -> EnrichmentFailureCause.WordProblem(InputProblem.Unintelligible)
    CONTRADICTORY -> EnrichmentFailureCause.WordProblem(InputProblem.Contradictory)
    UNMAPPABLE -> EnrichmentFailureCause.WordProblem(InputProblem.Unmappable)
    CREDITS_EXHAUSTED -> EnrichmentFailureCause.CreditsExhausted
    else -> EnrichmentFailureCause.Technical
}

internal fun enrichmentFailureCauseOf(status: EnrichmentStatus, code: String?): EnrichmentFailureCause? {
    if (status != EnrichmentStatus.FAILED) return null
    return enrichmentFailureCauseFromWire(code)
}

internal fun enrichmentFailureOf(status: EnrichmentStatus, code: String?, reason: String?): EnrichmentFailure? {
    val cause: EnrichmentFailureCause = enrichmentFailureCauseOf(status, code) ?: return null
    return EnrichmentFailure(cause = cause, reason = reason)
}

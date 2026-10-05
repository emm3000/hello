package com.emm.hello.analytics

import com.emm.domain.generation.EnrichmentFailureCause

data class AnalyticsPayload(val name: String, val params: Map<String, Any>) {
    init {
        require(params.values.all { value -> value is String || value is Long }) {
            "Analytics params must be String or Long: $params"
        }
    }
}

fun ProductEvent.toPayload(): AnalyticsPayload = when (this) {
    ProductEvent.OnboardingCompleted -> AnalyticsPayload("onboarding_completed", emptyMap())
    is ProductEvent.WordCaptured -> AnalyticsPayload("word_captured", mapOf("mode" to mode.wireValue))
    is ProductEvent.StudySessionCompleted -> AnalyticsPayload(
        name = "study_session_completed",
        params = mapOf(
            "reviewed" to reviewed.toLong(),
            "knew" to knew.toLong(),
            "forgot" to forgot.toLong(),
            "scope" to scope.wireValue,
            "extra" to isExtra.toString(),
        ),
    )
    is ProductEvent.DailyNewCardLimitSelected ->
        AnalyticsPayload("daily_new_card_limit_selected", mapOf("limit" to limit.toLong()))
    is ProductEvent.ExtraNewCardsRequested ->
        AnalyticsPayload("extra_new_cards_requested", mapOf("source" to source.wireValue))
    is ProductEvent.CuratedDeckInstalled ->
        AnalyticsPayload("curated_deck_installed", mapOf("deck_id" to curatedDeckId))
    is ProductEvent.EnrichmentFailed -> AnalyticsPayload("enrichment_failed", mapOf("cause" to cause.wireValue()))
}

private fun EnrichmentFailureCause.wireValue(): String = when (this) {
    EnrichmentFailureCause.Technical -> "technical"
    EnrichmentFailureCause.AppCheckRejected -> "app_check_rejected"
    is EnrichmentFailureCause.WordProblem -> "word_problem"
    EnrichmentFailureCause.CreditsExhausted -> "credits_exhausted"
}

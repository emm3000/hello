package com.emm.hello.analytics

import com.emm.domain.generation.EnrichmentFailureCause

sealed interface ProductEvent {

    data object OnboardingCompleted : ProductEvent

    data class WordCaptured(val mode: CaptureMode) : ProductEvent

    data class StudySessionCompleted(
        val reviewed: Int,
        val knew: Int,
        val forgot: Int,
        val scope: StudyScope,
        val isExtra: Boolean,
    ) : ProductEvent

    data class DailyNewCardLimitSelected(val limit: Int) : ProductEvent

    data class ExtraNewCardsRequested(val source: ExtraNewCardsSource) : ProductEvent

    data class CuratedDeckInstalled(val curatedDeckId: String) : ProductEvent

    data class EnrichmentFailed(val cause: EnrichmentFailureCause) : ProductEvent
}

enum class CaptureMode(val wireValue: String) {
    AI("ai"),
    MANUAL("manual"),
}

enum class StudyScope(val wireValue: String) {
    ALL("all"),
    DECK("deck"),
}

enum class ExtraNewCardsSource(val wireValue: String) {
    TODAY("today"),
    SESSION_END("session_end"),
}

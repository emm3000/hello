package com.emm.hello.analytics

import com.emm.domain.generation.EnrichmentFailureCause
import com.emm.domain.generation.InputProblem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnalyticsPayloadTest {

    @Test
    fun `onboarding completed carries no params`() {
        val payload: AnalyticsPayload = ProductEvent.OnboardingCompleted.toPayload()

        assertThat(payload).isEqualTo(AnalyticsPayload("onboarding_completed", emptyMap()))
    }

    @Test
    fun `word captured carries the capture mode`() {
        assertThat(ProductEvent.WordCaptured(CaptureMode.AI).toPayload())
            .isEqualTo(AnalyticsPayload("word_captured", mapOf("mode" to "ai")))
        assertThat(ProductEvent.WordCaptured(CaptureMode.MANUAL).toPayload())
            .isEqualTo(AnalyticsPayload("word_captured", mapOf("mode" to "manual")))
    }

    @Test
    fun `study session completed carries counts as longs and scope and extra as strings`() {
        val payload: AnalyticsPayload = ProductEvent.StudySessionCompleted(
            reviewed = 12,
            knew = 9,
            forgot = 3,
            scope = StudyScope.DECK,
            isExtra = true,
        ).toPayload()

        assertThat(payload).isEqualTo(
            AnalyticsPayload(
                name = "study_session_completed",
                params = mapOf(
                    "reviewed" to 12L,
                    "knew" to 9L,
                    "forgot" to 3L,
                    "scope" to "deck",
                    "extra" to "true",
                ),
            )
        )
        assertThat(
            ProductEvent.StudySessionCompleted(1, 1, 0, StudyScope.ALL, isExtra = false).toPayload().params
        ).containsAtLeast("scope", "all", "extra", "false")
    }

    @Test
    fun `daily new card limit selected carries the limit as a long`() {
        assertThat(ProductEvent.DailyNewCardLimitSelected(20).toPayload())
            .isEqualTo(AnalyticsPayload("daily_new_card_limit_selected", mapOf("limit" to 20L)))
    }

    @Test
    fun `extra new cards requested carries the source`() {
        assertThat(ProductEvent.ExtraNewCardsRequested(ExtraNewCardsSource.TODAY).toPayload())
            .isEqualTo(AnalyticsPayload("extra_new_cards_requested", mapOf("source" to "today")))
        assertThat(ProductEvent.ExtraNewCardsRequested(ExtraNewCardsSource.SESSION_END).toPayload())
            .isEqualTo(AnalyticsPayload("extra_new_cards_requested", mapOf("source" to "session_end")))
    }

    @Test
    fun `curated deck installed carries the curated deck id`() {
        assertThat(ProductEvent.CuratedDeckInstalled("spanish-traps").toPayload())
            .isEqualTo(AnalyticsPayload("curated_deck_installed", mapOf("deck_id" to "spanish-traps")))
    }

    @Test
    fun `enrichment failed maps every domain cause`() {
        val causes: Map<EnrichmentFailureCause, String> = mapOf(
            EnrichmentFailureCause.Technical to "technical",
            EnrichmentFailureCause.AppCheckRejected to "app_check_rejected",
            EnrichmentFailureCause.WordProblem(InputProblem.Unintelligible) to "word_problem",
            EnrichmentFailureCause.CreditsExhausted to "credits_exhausted",
        )

        causes.forEach { (cause, expected) ->
            assertThat(ProductEvent.EnrichmentFailed(cause).toPayload())
                .isEqualTo(AnalyticsPayload("enrichment_failed", mapOf("cause" to expected)))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a param that is neither String nor Long is rejected`() {
        AnalyticsPayload("bad", mapOf("count" to 1))
    }
}

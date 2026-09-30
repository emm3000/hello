package com.emm.domain.generation

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnrichmentFailureCauseTest {

    @Test
    fun `a technical failure can be retried right away`() {
        assertTrue(EnrichmentFailureCause.Technical.canRetryAt(NOW, credits = null))
    }

    @Test
    fun `an app check rejection cannot be retried`() {
        assertFalse(EnrichmentFailureCause.AppCheckRejected.canRetryAt(NOW, credits = null))
    }

    @Test
    fun `a word problem cannot be retried whatever the problem`() {
        InputProblem.entries.forEach { problem ->
            assertFalse(EnrichmentFailureCause.WordProblem(problem).canRetryAt(NOW, credits = null))
        }
    }

    @Test
    fun `exhausted credits cannot be retried before the reset`() {
        val credits = GenerationCredits(remaining = 0, resetAt = NOW.plusSeconds(1))

        assertFalse(EnrichmentFailureCause.CreditsExhausted.canRetryAt(NOW, credits))
    }

    @Test
    fun `exhausted credits can be retried at the reset instant`() {
        val credits = GenerationCredits(remaining = 0, resetAt = NOW)

        assertTrue(EnrichmentFailureCause.CreditsExhausted.canRetryAt(NOW, credits))
    }

    @Test
    fun `exhausted credits can be retried when the reset is unknown`() {
        assertTrue(EnrichmentFailureCause.CreditsExhausted.canRetryAt(NOW, credits = null))
    }

    @Test
    fun `every refusal code maps to its cause`() {
        val expected: Map<GenerationRefusalCode, EnrichmentFailureCause> = mapOf(
            GenerationRefusalCode.EmptyInput to EnrichmentFailureCause.WordProblem(InputProblem.EmptyInput),
            GenerationRefusalCode.Unintelligible to EnrichmentFailureCause.WordProblem(InputProblem.Unintelligible),
            GenerationRefusalCode.Contradictory to EnrichmentFailureCause.WordProblem(InputProblem.Contradictory),
            GenerationRefusalCode.Unmappable to EnrichmentFailureCause.WordProblem(InputProblem.Unmappable),
            GenerationRefusalCode.CreditsExhausted to EnrichmentFailureCause.CreditsExhausted,
        )

        assertEquals(expected, GenerationRefusalCode.entries.associateWith { it.toFailureCause() })
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-10T20:00:00Z")
    }
}

package com.emm.hello.enrichment

import com.emm.domain.generation.AmbiguousGenerationInputException
import com.emm.domain.generation.GenerationCreditsExhaustedException
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class EnrichmentFailureReasonTest {

    @Test
    fun `an ambiguous input error carries its reason to the card`() {
        val error = AmbiguousGenerationInputException(reason = "No entendí el texto")

        assertThat(EnrichmentFailureReason.of(error)).isEqualTo("No entendí el texto")
    }

    @Test
    fun `exhausted credits carry the server reason to the card`() {
        val error = GenerationCreditsExhaustedException(
            resetAt = Instant.parse("2026-09-08T00:00:00Z"),
            reason = "Alcanzaste el límite diario",
        )

        assertThat(EnrichmentFailureReason.of(error)).isEqualTo("Alcanzaste el límite diario")
    }

    @Test
    fun `any other error leaves the card without a reason`() {
        val error = IllegalStateException("boom")

        assertThat(EnrichmentFailureReason.of(error)).isNull()
    }
}

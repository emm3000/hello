package com.emm.hello.enrichment

import com.emm.domain.generation.AmbiguousGenerationInputException
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.generation.GenerationCreditsExhaustedException
import com.emm.domain.generation.GenerationRefusalCode
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class EnrichmentFailuresTest {

    @Test
    fun `an ambiguous input error carries its reason to the card`() {
        val error = AmbiguousGenerationInputException(reason = "No entendí el texto")

        assertThat(EnrichmentFailures.of(error)).isEqualTo(EnrichmentFailure(null, "No entendí el texto"))
    }

    @Test
    fun `an ambiguous input error carries its typed code to the card`() {
        val error = AmbiguousGenerationInputException(
            reason = "No entendí el texto",
            code = GenerationRefusalCode.Unintelligible,
        )

        assertThat(EnrichmentFailures.of(error))
            .isEqualTo(EnrichmentFailure(GenerationRefusalCode.Unintelligible, "No entendí el texto"))
    }

    @Test
    fun `exhausted credits carry the server reason and the credits code to the card`() {
        val error = GenerationCreditsExhaustedException(
            resetAt = Instant.parse("2026-09-08T00:00:00Z"),
            reason = "Alcanzaste el límite diario",
        )

        assertThat(EnrichmentFailures.of(error))
            .isEqualTo(EnrichmentFailure(GenerationRefusalCode.CreditsExhausted, "Alcanzaste el límite diario"))
    }

    @Test
    fun `any other error leaves the card without a failure`() {
        val error = IllegalStateException("boom")

        assertThat(EnrichmentFailures.of(error)).isNull()
    }
}

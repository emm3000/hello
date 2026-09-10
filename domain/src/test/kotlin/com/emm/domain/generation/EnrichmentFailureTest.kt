package com.emm.domain.generation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnrichmentFailureTest {

    @Test
    fun `of returns null when neither a code nor a reason is known`() {
        assertNull(EnrichmentFailure.of(code = null, reason = null))
    }

    @Test
    fun `of keeps a code without a reason`() {
        val failure: EnrichmentFailure? = EnrichmentFailure.of(
            code = GenerationRefusalCode.Unintelligible,
            reason = null,
        )

        assertEquals(EnrichmentFailure(GenerationRefusalCode.Unintelligible, null), failure)
    }

    @Test
    fun `of keeps a reason without a code`() {
        val failure: EnrichmentFailure? = EnrichmentFailure.of(code = null, reason = "No entendí el texto")

        assertEquals(EnrichmentFailure(null, "No entendí el texto"), failure)
    }

    @Test
    fun `of keeps both a code and a reason`() {
        val failure: EnrichmentFailure? = EnrichmentFailure.of(
            code = GenerationRefusalCode.CreditsExhausted,
            reason = "Alcanzaste el límite diario",
        )

        assertEquals(
            EnrichmentFailure(GenerationRefusalCode.CreditsExhausted, "Alcanzaste el límite diario"),
            failure,
        )
    }
}

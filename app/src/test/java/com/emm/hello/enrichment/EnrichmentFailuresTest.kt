package com.emm.hello.enrichment

import com.emm.domain.generation.AmbiguousGenerationInputException
import com.emm.domain.generation.AppCheckRejectedException
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.generation.EnrichmentFailureCause
import com.emm.domain.generation.GenerationCreditsExhaustedException
import com.emm.domain.generation.GenerationRefusalCode
import com.emm.domain.generation.InputProblem
import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.time.Instant
import org.junit.Test

class EnrichmentFailuresTest {

    @Test
    fun `an app check rejection is stored as app check rejected`() {
        val error = AppCheckRejectedException(IllegalStateException("app_check_failed"))

        assertThat(EnrichmentFailures.of(error))
            .isEqualTo(EnrichmentFailure(EnrichmentFailureCause.AppCheckRejected, null))
    }

    @Test
    fun `an ambiguous input error carries its word problem and reason to the card`() {
        val error = AmbiguousGenerationInputException(
            reason = "No entendí el texto",
            code = GenerationRefusalCode.Unintelligible,
        )

        assertThat(EnrichmentFailures.of(error)).isEqualTo(
            EnrichmentFailure(EnrichmentFailureCause.WordProblem(InputProblem.Unintelligible), "No entendí el texto"),
        )
    }

    @Test
    fun `an ambiguous input error without a code is stored as an unintelligible word`() {
        val error = AmbiguousGenerationInputException(reason = "No entendí el texto")

        assertThat(EnrichmentFailures.of(error)).isEqualTo(
            EnrichmentFailure(EnrichmentFailureCause.WordProblem(InputProblem.Unintelligible), "No entendí el texto"),
        )
    }

    @Test
    fun `exhausted credits carry the server reason and the credits cause to the card`() {
        val error = GenerationCreditsExhaustedException(
            resetAt = Instant.parse("2026-09-08T00:00:00Z"),
            reason = "Alcanzaste el límite diario",
        )

        assertThat(EnrichmentFailures.of(error))
            .isEqualTo(EnrichmentFailure(EnrichmentFailureCause.CreditsExhausted, "Alcanzaste el límite diario"))
    }

    @Test
    fun `a domain validation error is stored as technical`() {
        val error = DomainValidationException(
            issues = listOf(ValidationIssue.Error(code = IssueCode.DuplicateWordInDeck, field = "word")),
        )

        assertThat(EnrichmentFailures.of(error))
            .isEqualTo(EnrichmentFailure(EnrichmentFailureCause.Technical, null))
    }

    @Test
    fun `any other error is stored as technical`() {
        val error = IOException("generate function returned 500")

        assertThat(EnrichmentFailures.of(error))
            .isEqualTo(EnrichmentFailure(EnrichmentFailureCause.Technical, null))
    }
}

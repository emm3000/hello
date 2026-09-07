package com.emm.hello.enrichment

import com.emm.domain.generation.AmbiguousGenerationInputException
import com.emm.domain.generation.AppCheckRejectedException
import com.emm.domain.generation.GenerationQuotaExceededException
import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class EnrichmentRetryPolicyTest {

    @Test
    fun `shouldRetry is false for a validation error`() {
        val error = DomainValidationException(
            listOf(ValidationIssue.Error(IssueCode.MissingUsagePattern, "usage_pattern")),
        )

        assertThat(EnrichmentRetryPolicy.shouldRetry(error)).isFalse()
    }

    @Test
    fun `shouldRetry is false for a quota error`() {
        val error = GenerationQuotaExceededException(limit = 20, resetAt = Instant.EPOCH)

        assertThat(EnrichmentRetryPolicy.shouldRetry(error)).isFalse()
    }

    @Test
    fun `shouldRetry is false for an ambiguous input error`() {
        val error = AmbiguousGenerationInputException(reason = "No entendi el texto")

        assertThat(EnrichmentRetryPolicy.shouldRetry(error)).isFalse()
    }

    @Test
    fun `shouldRetry is false for an App Check rejection`() {
        val error = AppCheckRejectedException(IllegalStateException("Firebase App Check token is invalid."))

        assertThat(EnrichmentRetryPolicy.shouldRetry(error)).isFalse()
    }

    @Test
    fun `shouldRetry is true for any other error`() {
        val error = IllegalStateException("boom")

        assertThat(EnrichmentRetryPolicy.shouldRetry(error)).isTrue()
    }
}

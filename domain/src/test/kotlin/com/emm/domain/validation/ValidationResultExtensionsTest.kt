package com.emm.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ValidationResultExtensionsTest {

    @Test
    fun `requireValid returns value for valid result`() {
        val value = ValidationResult.valid(value = "ok").requireValid()

        assertEquals("ok", value)
    }

    @Test
    fun `requireValid throws DomainValidationException for invalid result`() {
        val invalid = ValidationResult.invalid(
            value = "bad",
            errors = listOf(
                ValidationIssue.Error(
                    code = IssueCode.EmptyUserText,
                    field = "userText",
                )
            ),
        )

        assertFailsWith<DomainValidationException> {
            invalid.requireValid()
        }
    }
}

package com.emm.domain.validation

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationResultTest {

    @Test
    fun `valid result can carry warnings and remain valid`() {
        val warning = ValidationIssue.Warning(
            code = IssueCode.ContextSentenceTooShort,
            field = "contextSentence",
        )

        val result = ValidationResult.valid(
            value = "normalized",
            warnings = listOf(warning),
        )

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertEquals(listOf(warning), result.warnings)
    }

    @Test
    fun `invalid result exposes errors and warnings`() {
        val error = ValidationIssue.Error(
            code = IssueCode.EmptyUserText,
            field = "userText",
        )
        val warning = ValidationIssue.Warning(
            code = IssueCode.WordInputContainsWhitespace,
            field = "userText",
        )

        val result = ValidationResult.invalid(
            value = "normalized",
            errors = listOf(error),
            warnings = listOf(warning),
        )

        assertFalse(result.isValid)
        assertEquals(listOf(error), result.errors)
        assertEquals(listOf(warning), result.warnings)
    }

    @Test
    fun `invalid result requires at least one error`() {
        assertFailsWith<IllegalArgumentException> {
            ValidationResult.Invalid(
                value = "normalized",
                issues = listOf(
                    ValidationIssue.Warning(
                        code = IssueCode.ContextSentenceTooShort,
                        field = "contextSentence",
                    )
                ),
            )
        }
    }

    @Test
    fun `validation issue requires a field pointer`() {
        assertFailsWith<IllegalArgumentException> {
            ValidationIssue.Error(
                code = IssueCode.EmptyUserText,
                field = "   ",
            )
        }
    }
}

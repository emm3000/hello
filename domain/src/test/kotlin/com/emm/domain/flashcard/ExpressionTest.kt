package com.emm.domain.flashcard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExpressionTest {

    @Test
    fun `toExpression trims and normalizes whitespace`() {
        val expression = "  take   off  ".toExpression()

        assertEquals("take off", expression.value)
        assertEquals("take off", expression.canonical)
    }

    @Test
    fun `toExpression keeps original case in value and lowers canonical`() {
        val expression = "Run Fast".toExpression()

        assertEquals("Run Fast", expression.value)
        assertEquals("run fast", expression.canonical)
    }

    @Test
    fun `toExpression rejects blank input`() {
        assertFailsWith<IllegalArgumentException> {
            "   ".toExpression()
        }
    }
}

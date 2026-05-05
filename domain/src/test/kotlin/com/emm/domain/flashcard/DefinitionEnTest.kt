package com.emm.domain.flashcard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DefinitionEnTest {

    @Test
    fun `toDefinitionEn trims and normalizes whitespace`() {
        val definition = "  to   move very  fast  ".toDefinitionEn()

        assertEquals("to move very fast", definition.value)
        assertEquals("to move very fast", definition.canonical)
    }

    @Test
    fun `toDefinitionEn keeps case in value and lowers canonical`() {
        val definition = "To Make Something Clear".toDefinitionEn()

        assertEquals("To Make Something Clear", definition.value)
        assertEquals("to make something clear", definition.canonical)
    }

    @Test
    fun `toDefinitionEn rejects blank input`() {
        assertFailsWith<IllegalArgumentException> {
            "   ".toDefinitionEn()
        }
    }
}

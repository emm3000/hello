package com.emm.domain.flashcard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntendedMeaningEsTest {

    @Test
    fun `toIntendedMeaningEs trims and normalizes whitespace`() {
        val meaning = "  pedir   prestado  ".toIntendedMeaningEs()

        assertEquals("pedir prestado", meaning.value)
        assertEquals("pedir prestado", meaning.canonical)
    }

    @Test
    fun `toIntendedMeaningEs keeps accents in canonical form`() {
        val meaning = "Acción".toIntendedMeaningEs()

        assertEquals("Acción", meaning.value)
        assertEquals("acción", meaning.canonical)
    }

    @Test
    fun `toIntendedMeaningEs rejects blank input`() {
        assertFailsWith<IllegalArgumentException> {
            "   ".toIntendedMeaningEs()
        }
    }
}

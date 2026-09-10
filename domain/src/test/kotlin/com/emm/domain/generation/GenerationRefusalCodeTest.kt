package com.emm.domain.generation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GenerationRefusalCodeTest {

    @Test
    fun `fromWire maps every known backend code`() {
        assertEquals(GenerationRefusalCode.EmptyInput, GenerationRefusalCode.fromWire("empty_input"))
        assertEquals(GenerationRefusalCode.Unintelligible, GenerationRefusalCode.fromWire("unintelligible"))
        assertEquals(GenerationRefusalCode.Contradictory, GenerationRefusalCode.fromWire("contradictory"))
        assertEquals(GenerationRefusalCode.Unmappable, GenerationRefusalCode.fromWire("unmappable"))
        assertEquals(GenerationRefusalCode.CreditsExhausted, GenerationRefusalCode.fromWire("credits_exhausted"))
    }

    @Test
    fun `fromWire returns null for an unknown code`() {
        assertNull(GenerationRefusalCode.fromWire("banana"))
    }

    @Test
    fun `fromWire returns null for a missing code`() {
        assertNull(GenerationRefusalCode.fromWire(null))
    }

    @Test
    fun `fromWire is case sensitive`() {
        assertNull(GenerationRefusalCode.fromWire("EMPTY_INPUT"))
    }

    @Test
    fun `every code round trips through its wire value`() {
        GenerationRefusalCode.entries.forEach { code ->
            assertEquals(code, GenerationRefusalCode.fromWire(code.wire))
        }
    }
}

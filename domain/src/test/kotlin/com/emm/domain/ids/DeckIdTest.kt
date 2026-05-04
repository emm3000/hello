package com.emm.domain.ids

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeckIdTest {

    @Test
    fun `toDeckId trims boundary whitespace`() {
        val deckId = "  deck-123  ".toDeckId()

        assertEquals("deck-123", deckId.value)
    }

    @Test
    fun `toDeckId rejects blank input`() {
        assertFailsWith<IllegalArgumentException> {
            "   ".toDeckId()
        }
    }
}

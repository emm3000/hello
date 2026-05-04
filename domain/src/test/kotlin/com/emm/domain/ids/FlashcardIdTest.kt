package com.emm.domain.ids

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlashcardIdTest {

    @Test
    fun `toFlashcardId trims boundary whitespace`() {
        val flashcardId = "  flashcard-123  ".toFlashcardId()

        assertEquals("flashcard-123", flashcardId.value)
    }

    @Test
    fun `toFlashcardId rejects blank input`() {
        assertFailsWith<IllegalArgumentException> {
            "   ".toFlashcardId()
        }
    }
}

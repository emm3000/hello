package com.emm.domain.flashcard

import com.emm.domain.generation.LearningNoteType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExactDuplicateKeyTest {

    @Test
    fun `from normalizes deck expression and meaning`() {
        val key = ExactDuplicateKey.from(
            deckId = "  deck-1  ",
            expression = "  Take   Off ",
            intendedMeaningEs = "  despegar   ",
            noteType = LearningNoteType.PhrasalVerb,
        )

        assertEquals("deck-1", key.deckId.value)
        assertEquals("Take Off", key.expression.value)
        assertEquals("take off", key.expression.canonical)
        assertEquals("despegar", key.intendedMeaningEs.value)
        assertEquals("despegar", key.intendedMeaningEs.canonical)
        assertEquals("deck-1|take off|despegar|phrasalverb", key.canonicalValue)
    }

    @Test
    fun `from rejects blank deckId`() {
        assertFailsWith<IllegalArgumentException> {
            ExactDuplicateKey.from(
                deckId = "   ",
                expression = "run",
                intendedMeaningEs = "correr",
                noteType = LearningNoteType.Word,
            )
        }
    }

    @Test
    fun `from rejects blank expression`() {
        assertFailsWith<IllegalArgumentException> {
            ExactDuplicateKey.from(
                deckId = "deck-1",
                expression = "   ",
                intendedMeaningEs = "correr",
                noteType = LearningNoteType.Word,
            )
        }
    }

    @Test
    fun `from rejects blank intendedMeaningEs`() {
        assertFailsWith<IllegalArgumentException> {
            ExactDuplicateKey.from(
                deckId = "deck-1",
                expression = "run",
                intendedMeaningEs = "   ",
                noteType = LearningNoteType.Word,
            )
        }
    }
}

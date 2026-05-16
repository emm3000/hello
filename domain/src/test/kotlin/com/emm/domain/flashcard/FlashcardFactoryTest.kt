package com.emm.domain.flashcard

import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlashcardFactoryTest {

    private val review = FlashcardReview.empty(SystemClock)

    @Test
    fun `create accepts valid word meaning and translation`() {
        val card = Flashcard.create(
            id = "card-1".toFlashcardId(),
            word = "borrow",
            meaning = "to take something and return it later",
            translation = "pedir prestado",
            review = review,
        )

        assertEquals("borrow", card.word)
        assertEquals("to take something and return it later", card.meaning)
        assertEquals("pedir prestado", card.translation)
    }

    @Test
    fun `create normalizes whitespace in the three critical fields`() {
        val card = Flashcard.create(
            id = "card-1".toFlashcardId(),
            word = "  Borrow   ",
            meaning = "  to   take something   and return it later  ",
            translation = "  pedir   prestado  ",
            review = review,
        )

        assertEquals("Borrow", card.word)
        assertEquals("to take something and return it later", card.meaning)
        assertEquals("pedir prestado", card.translation)
    }

    @Test
    fun `create rejects blank word`() {
        assertFailsWith<IllegalArgumentException> {
            Flashcard.create(
                id = "card-1".toFlashcardId(),
                word = "   ",
                meaning = "valid meaning",
                translation = "traducción válida",
                review = review,
            )
        }
    }

    @Test
    fun `create rejects blank meaning`() {
        assertFailsWith<IllegalArgumentException> {
            Flashcard.create(
                id = "card-1".toFlashcardId(),
                word = "borrow",
                meaning = "",
                translation = "pedir prestado",
                review = review,
            )
        }
    }

    @Test
    fun `create rejects blank translation`() {
        assertFailsWith<IllegalArgumentException> {
            Flashcard.create(
                id = "card-1".toFlashcardId(),
                word = "borrow",
                meaning = "valid meaning",
                translation = "",
                review = review,
            )
        }
    }

    @Test
    fun `create preserves optional fields without normalizing them`() {
        val card = Flashcard.create(
            id = "card-1".toFlashcardId(),
            word = "borrow",
            meaning = "valid meaning",
            translation = "pedir prestado",
            review = review,
            phonetic = "/ˈbɒr.əʊ/",
            partOfSpeech = "verb",
            irregularForms = listOf("borrowed", "borrowing"),
        )

        assertEquals("/ˈbɒr.əʊ/", card.phonetic)
        assertEquals("verb", card.partOfSpeech)
        assertEquals(listOf("borrowed", "borrowing"), card.irregularForms)
    }

    @Test
    fun `empty constructor still allowed for placeholder use`() {
        val empty = Flashcard.empty(SystemClock)

        assertEquals("", empty.word)
        assertEquals("", empty.meaning)
        assertEquals("", empty.translation)
    }
}

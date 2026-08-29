package com.emm.data.suggestion

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WordSuggestionResponseParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parse maps a valid JSON response to domain`() {
        val raw = """
            {"situation": "Ordering food at a busy restaurant", "words": [{"word": "the check", "translation": "la cuenta"}]}
        """.trimIndent()

        val result = WordSuggestionResponseParser.parse(raw, json)

        assertEquals("Ordering food at a busy restaurant", result.situation)
        assertEquals(1, result.words.size)
        assertEquals("the check", result.words[0].word)
        assertEquals("la cuenta", result.words[0].translation)
    }

    @Test
    fun `parse strips a fenced json block`() {
        val raw = """
            ```json
            {"situation": "Asking for directions", "words": [{"word": "turn left", "translation": "doble a la izquierda"}]}
            ```
        """.trimIndent()

        val result = WordSuggestionResponseParser.parse(raw, json)

        assertEquals("Asking for directions", result.situation)
        assertEquals(1, result.words.size)
        assertEquals("turn left", result.words[0].word)
    }

    @Test
    fun `parse throws when situation is blank`() {
        val raw = """{"situation": "", "words": [{"word": "hi", "translation": "hola"}]}"""

        assertThrows(IllegalStateException::class.java) {
            WordSuggestionResponseParser.parse(raw, json)
        }
    }

    @Test
    fun `parse throws when words is empty`() {
        val raw = """{"situation": "Ordering food", "words": []}"""

        assertThrows(IllegalStateException::class.java) {
            WordSuggestionResponseParser.parse(raw, json)
        }
    }
}

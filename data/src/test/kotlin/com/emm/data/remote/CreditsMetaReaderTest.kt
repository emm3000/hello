package com.emm.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreditsMetaReaderTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `remainingOrNull reads the count from a generate-note body`() {
        val body = """
            {
              "success": true,
              "data": { "note_id": "note-1", "expression": "pick up" },
              "error": null,
              "meta": {
                "cached": false,
                "provider": "gemini",
                "model": "gemini-3.1-flash-lite",
                "credits_remaining": 3,
                "prompt_version": 2,
                "schema_version": 1
              }
            }
        """.trimIndent()

        assertEquals(3, CreditsMetaReader.remainingOrNull(body, json))
    }

    @Test
    fun `remainingOrNull reads the count from a suggest-words body`() {
        val body = """
            {
              "situation": "Ordering food at a busy restaurant",
              "words": [{ "word": "the check", "translation": "la cuenta" }],
              "meta": {
                "cached": true,
                "provider": "gemini",
                "model": "gemini-3.1-flash-lite",
                "credits_remaining": 17,
                "prompt_version": 1,
                "schema_version": 1
              }
            }
        """.trimIndent()

        assertEquals(17, CreditsMetaReader.remainingOrNull(body, json))
    }

    @Test
    fun `remainingOrNull reads a zero count`() {
        val body = "{\"success\":true,\"meta\":{\"credits_remaining\":0}}"

        assertEquals(0, CreditsMetaReader.remainingOrNull(body, json))
    }

    @Test
    fun `remainingOrNull returns null when meta is null`() {
        val body = """
            {
              "success": false,
              "data": null,
              "error": { "code": "credits_exhausted", "message": "Sin creditos" },
              "meta": null
            }
        """.trimIndent()

        assertNull(CreditsMetaReader.remainingOrNull(body, json))
    }

    @Test
    fun `remainingOrNull returns null when meta carries no credits field`() {
        val body = "{\"success\":true,\"meta\":{\"cached\":false,\"provider\":\"gemini\"}}"

        assertNull(CreditsMetaReader.remainingOrNull(body, json))
    }

    @Test
    fun `remainingOrNull returns null when the body has no meta at all`() {
        val body = "{\"success\":true,\"data\":null}"

        assertNull(CreditsMetaReader.remainingOrNull(body, json))
    }

    @Test
    fun `remainingOrNull returns null for a non json body`() {
        assertNull(CreditsMetaReader.remainingOrNull("<html>Gateway is down</html>", json))
    }

    @Test
    fun `remainingOrNull returns null for an empty body`() {
        assertNull(CreditsMetaReader.remainingOrNull("", json))
    }
}

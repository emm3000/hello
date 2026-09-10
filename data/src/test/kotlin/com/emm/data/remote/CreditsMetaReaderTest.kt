package com.emm.data.remote

import com.emm.domain.generation.GenerationCredits
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreditsMetaReaderTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `readOrNull reads the balance and the reset from a generate-note body`() {
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
                "reset_at": "2026-09-11T00:00:00Z",
                "prompt_version": 2,
                "schema_version": 1
              }
            }
        """.trimIndent()

        assertEquals(
            GenerationCredits(remaining = 3, resetAt = Instant.parse("2026-09-11T00:00:00Z")),
            CreditsMetaReader.readOrNull(body, json),
        )
    }

    @Test
    fun `readOrNull reads the balance and the reset from a suggest-words body`() {
        val body = """
            {
              "situation": "Ordering food at a busy restaurant",
              "words": [{ "word": "the check", "translation": "la cuenta" }],
              "meta": {
                "cached": true,
                "provider": "gemini",
                "credits_remaining": 17,
                "reset_at": "2026-09-11T00:00:00.000Z"
              }
            }
        """.trimIndent()

        assertEquals(
            GenerationCredits(remaining = 17, resetAt = Instant.parse("2026-09-11T00:00:00Z")),
            CreditsMetaReader.readOrNull(body, json),
        )
    }

    @Test
    fun `readOrNull reads an exhausted balance from a payment required body`() {
        val body = """
            {
              "success": false,
              "data": null,
              "error": {
                "code": "credits_exhausted",
                "message": "Sin creditos",
                "reset_at": "2026-09-11T00:00:00Z"
              },
              "meta": { "credits_remaining": 0, "reset_at": "2026-09-11T00:00:00Z" }
            }
        """.trimIndent()

        assertEquals(
            GenerationCredits(remaining = 0, resetAt = Instant.parse("2026-09-11T00:00:00Z")),
            CreditsMetaReader.readOrNull(body, json),
        )
    }

    @Test
    fun `readOrNull coerces a negative balance to zero`() {
        val body = "{\"meta\":{\"credits_remaining\":-4,\"reset_at\":\"2026-09-11T00:00:00Z\"}}"

        assertEquals(
            GenerationCredits(remaining = 0, resetAt = Instant.parse("2026-09-11T00:00:00Z")),
            CreditsMetaReader.readOrNull(body, json),
        )
    }

    @Test
    fun `readOrNull returns null when meta is null`() {
        val body = """
            {
              "success": false,
              "data": null,
              "error": { "code": "app_check_rejected", "message": "Invalid token" },
              "meta": null
            }
        """.trimIndent()

        assertNull(CreditsMetaReader.readOrNull(body, json))
    }

    @Test
    fun `readOrNull returns null when the body has no meta at all`() {
        assertNull(CreditsMetaReader.readOrNull("{\"success\":true,\"data\":null}", json))
    }

    @Test
    fun `readOrNull returns null when the balance is null`() {
        val body = "{\"meta\":{\"credits_remaining\":null,\"reset_at\":\"2026-09-11T00:00:00Z\"}}"

        assertNull(CreditsMetaReader.readOrNull(body, json))
    }

    @Test
    fun `readOrNull returns null when the reset is missing`() {
        assertNull(CreditsMetaReader.readOrNull("{\"meta\":{\"credits_remaining\":5}}", json))
    }

    @Test
    fun `readOrNull returns null when the reset does not parse as an instant`() {
        val body = "{\"meta\":{\"credits_remaining\":5,\"reset_at\":\"tomorrow at midnight\"}}"

        assertNull(CreditsMetaReader.readOrNull(body, json))
    }

    @Test
    fun `readOrNull returns null for a non json body`() {
        assertNull(CreditsMetaReader.readOrNull("<html>Gateway is down</html>", json))
    }

    @Test
    fun `readOrNull returns null for an empty body`() {
        assertNull(CreditsMetaReader.readOrNull("", json))
    }
}

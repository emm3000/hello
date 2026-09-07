package com.emm.data.remote

import com.emm.domain.generation.AppCheckRejectedException
import com.emm.domain.generation.GenerationCreditsExhaustedException
import com.emm.domain.generation.SessionExpiredException
import java.io.IOException
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionsReplyMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `map hands a successful body to the parser`() {
        val reply = FunctionsReply(status = 200, body = "{\"success\":true}")

        val parsed: String = FunctionsReplyMapper.map(reply, json) { body -> body.uppercase() }

        assertEquals("{\"SUCCESS\":TRUE}", parsed)
    }

    @Test
    fun `map turns unauthorized into an App Check rejection`() {
        val reply = FunctionsReply(
            status = 401,
            body = "{\"success\":false,\"error\":{\"code\":\"app_check_rejected\",\"message\":\"Invalid token\"}}",
        )

        val error: AppCheckRejectedException = assertThrows(AppCheckRejectedException::class.java) {
            FunctionsReplyMapper.map(reply, json, ::failOnSuccess)
        }

        assertEquals("app_check_rejected", error.message)
    }

    @Test
    fun `map turns an unauthorized gateway body into an expired session`() {
        val reply = FunctionsReply(status = 401, body = "{\"msg\":\"Invalid JWT\"}")

        val error: SessionExpiredException = assertThrows(SessionExpiredException::class.java) {
            FunctionsReplyMapper.map(reply, json, ::failOnSuccess)
        }

        assertEquals("{\"msg\":\"Invalid JWT\"}", error.message)
    }

    @Test
    fun `map turns an unauthorized empty body into an expired session`() {
        val reply = FunctionsReply(status = 401, body = "")

        assertThrows(SessionExpiredException::class.java) {
            FunctionsReplyMapper.map(reply, json, ::failOnSuccess)
        }
    }

    @Test
    fun `map keeps an unauthorized envelope as its declared code`() {
        val reply = FunctionsReply(
            status = 401,
            body = "{\"success\":false,\"error\":{\"code\":\"unauthorized\",\"message\":\"Missing token\"}}",
        )

        val error: AppCheckRejectedException = assertThrows(AppCheckRejectedException::class.java) {
            FunctionsReplyMapper.map(reply, json, ::failOnSuccess)
        }

        assertEquals("unauthorized", error.message)
    }

    @Test
    fun `map turns payment required into exhausted credits`() {
        val reply = FunctionsReply(
            status = 402,
            body = "{\"success\":false,\"error\":{\"code\":\"credits_exhausted\",\"message\":\"Sin creditos\"}}",
        )

        val error: GenerationCreditsExhaustedException = assertThrows(
            GenerationCreditsExhaustedException::class.java,
        ) {
            FunctionsReplyMapper.map(reply, json, ::failOnSuccess)
        }

        assertNull(error.resetAt)
    }

    @Test
    fun `map reads the reset instant and the reason from a full payment required body`() {
        val reply = FunctionsReply(
            status = 402,
            body = "{\"success\":false,\"error\":{\"code\":\"credits_exhausted\"," +
                "\"message\":\"Alcanzaste el limite diario\",\"reset_at\":\"2026-09-08T00:00:00Z\"}}",
        )

        val error: GenerationCreditsExhaustedException = assertThrows(
            GenerationCreditsExhaustedException::class.java,
        ) {
            FunctionsReplyMapper.map(reply, json, ::failOnSuccess)
        }

        assertEquals(Instant.parse("2026-09-08T00:00:00Z"), error.resetAt)
        assertEquals("Alcanzaste el limite diario", error.reason)
        assertEquals("Alcanzaste el limite diario", error.message)
    }

    @Test
    fun `map yields null fields when payment required carries an empty body`() {
        val reply = FunctionsReply(status = 402, body = "")

        val error: GenerationCreditsExhaustedException = assertThrows(
            GenerationCreditsExhaustedException::class.java,
        ) {
            FunctionsReplyMapper.map(reply, json, ::failOnSuccess)
        }

        assertNull(error.resetAt)
        assertNull(error.reason)
        assertEquals("Generation credits exhausted", error.message)
    }

    @Test
    fun `map keeps the reason when the reset instant is malformed`() {
        val reply = FunctionsReply(
            status = 402,
            body = "{\"success\":false,\"error\":{\"code\":\"credits_exhausted\"," +
                "\"message\":\"Sin creditos\",\"reset_at\":\"manana\"}}",
        )

        val error: GenerationCreditsExhaustedException = assertThrows(
            GenerationCreditsExhaustedException::class.java,
        ) {
            FunctionsReplyMapper.map(reply, json, ::failOnSuccess)
        }

        assertNull(error.resetAt)
        assertEquals("Sin creditos", error.reason)
    }

    @Test
    fun `map turns a bad request into an illegal argument carrying the server message`() {
        val reply = FunctionsReply(
            status = 400,
            body = "{\"success\":false,\"error\":{\"code\":\"invalid_request\",\"message\":\"user_text is required\"}}",
        )

        val error: IllegalArgumentException = assertThrows(IllegalArgumentException::class.java) {
            FunctionsReplyMapper.map(reply, json, ::failOnSuccess)
        }

        assertEquals("user_text is required", error.message)
    }

    @Test
    fun `map turns providers exhausted into an io error`() {
        val reply = FunctionsReply(
            status = 503,
            body = "{\"success\":false,\"error\":{\"code\":\"providers_exhausted\",\"message\":\"No providers\"}}",
        )

        val error: IOException = assertThrows(IOException::class.java) {
            FunctionsReplyMapper.map(reply, json, ::failOnSuccess)
        }

        assertTrue(error.message.orEmpty().contains("503"))
    }

    @Test
    fun `map turns a server error into an io error`() {
        val reply = FunctionsReply(status = 500, body = "{\"success\":false,\"error\":{\"code\":\"internal\"}}")

        val error: IOException = assertThrows(IOException::class.java) {
            FunctionsReplyMapper.map(reply, json, ::failOnSuccess)
        }

        assertTrue(error.message.orEmpty().contains("500"))
    }

    @Test
    fun `map turns a non json server error body into an io error`() {
        val reply = FunctionsReply(status = 500, body = "<html>Gateway is down</html>")

        val error: IOException = assertThrows(IOException::class.java) {
            FunctionsReplyMapper.map(reply, json, ::failOnSuccess)
        }

        assertTrue(error.message.orEmpty().contains("500"))
    }

    @Test
    fun `map falls back to the raw body when a bad request carries no json`() {
        val reply = FunctionsReply(status = 400, body = "not json at all")

        val error: IllegalArgumentException = assertThrows(IllegalArgumentException::class.java) {
            FunctionsReplyMapper.map(reply, json, ::failOnSuccess)
        }

        assertEquals("not json at all", error.message)
    }
}

private fun failOnSuccess(body: String): Nothing = throw AssertionError("onSuccess ran for $body")

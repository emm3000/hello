package com.emm.data.suggestion

import com.emm.data.remote.AppCheckTokenProvider
import com.emm.data.remote.FunctionsReply
import com.emm.data.remote.FunctionsTransport
import com.emm.data.remote.SessionInitializer
import com.emm.domain.generation.AppCheckRejectedException
import com.emm.domain.suggestion.WordSuggestions
import com.emm.domain.telemetry.GenerationTelemetry
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteWordSuggestionRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `suggest posts the recent words and returns the parsed suggestions`() = runTest {
        val body = """
            {
              "situation": "Ordering food at a busy restaurant",
              "words": [{ "word": "the check", "translation": "la cuenta" }],
              "meta": { "cached": false, "provider": "gemini" }
            }
        """.trimIndent()
        val transport = RecordingFunctionsTransport(FunctionsReply(status = 200, body = body))
        val repository = repository(transport)

        val suggestions: WordSuggestions = repository.suggest(listOf("hello", "goodbye"))

        val payload: String = transport.body.toString()
        assertEquals("suggest-words", transport.function)
        assertTrue(payload, payload.contains("\"recent_words\":[\"hello\",\"goodbye\"]"))
        assertEquals("Ordering food at a busy restaurant", suggestions.situation)
        assertEquals(1, suggestions.words.size)
        assertEquals("la cuenta", suggestions.words.first().translation)
    }

    @Test
    fun `suggest surfaces an unauthorized reply as an App Check rejection`() = runTest {
        val body = "{\"success\":false,\"error\":{\"code\":\"app_check_rejected\",\"message\":\"Invalid token\"}}"
        val transport = RecordingFunctionsTransport(FunctionsReply(status = 401, body = body))
        val repository = repository(transport)

        val error: Throwable? = runCatching { repository.suggest(listOf("hello")) }.exceptionOrNull()

        assertTrue(error is AppCheckRejectedException)
    }

    private fun repository(transport: FunctionsTransport): RemoteWordSuggestionRepository {
        return RemoteWordSuggestionRepository(
            transport = transport,
            session = FakeSessionInitializer("session-jwt"),
            appCheck = FakeAppCheckTokenProvider("app-check-token"),
            telemetry = GenerationTelemetry.NoOp,
            json = json,
        )
    }
}

private class RecordingFunctionsTransport(
    private val reply: FunctionsReply,
) : FunctionsTransport {

    var function: String = ""
        private set
    var body: JsonElement = JsonNull
        private set

    override suspend fun invoke(
        function: String,
        body: JsonElement,
        accessToken: String,
        appCheckToken: String,
    ): FunctionsReply {
        this.function = function
        this.body = body
        return reply
    }
}

private class FakeSessionInitializer(private val accessToken: String) : SessionInitializer {
    override suspend fun ensureSession(): String = accessToken
}

private class FakeAppCheckTokenProvider(private val appCheckToken: String) : AppCheckTokenProvider {
    override suspend fun token(): String = appCheckToken
}

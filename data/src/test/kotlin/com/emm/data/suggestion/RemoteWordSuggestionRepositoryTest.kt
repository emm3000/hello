package com.emm.data.suggestion

import com.emm.data.remote.AppCheckTokenProvider
import com.emm.data.remote.FunctionsReply
import com.emm.data.remote.FunctionsTransport
import com.emm.data.remote.SessionInitializer
import com.emm.domain.generation.AppCheckRejectedException
import com.emm.domain.generation.GenerationCredits
import com.emm.domain.generation.GenerationCreditsExhaustedException
import com.emm.domain.generation.GenerationCreditsRepository
import com.emm.domain.suggestion.WordSuggestions
import com.emm.domain.telemetry.GenerationTelemetry
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
    fun `suggest ensures the session, posts the recent words and returns the parsed suggestions`() = runTest {
        val body = """
            {
              "situation": "Ordering food at a busy restaurant",
              "words": [{ "word": "the check", "translation": "la cuenta" }],
              "meta": { "cached": false, "provider": "gemini" }
            }
        """.trimIndent()
        val transport = RecordingFunctionsTransport(FunctionsReply(status = 200, body = body))
        val session = FakeSessionInitializer()
        val repository = repository(transport, session)

        val suggestions: WordSuggestions = repository.suggest(listOf("hello", "goodbye"))

        val payload: String = transport.body.toString()
        assertTrue(session.ensured)
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

    @Test
    fun `suggest records the balance and the reset from the reply meta`() = runTest {
        val body = """
            {
              "situation": "Ordering food at a busy restaurant",
              "words": [{ "word": "the check", "translation": "la cuenta" }],
              "meta": {
                "cached": false,
                "provider": "gemini",
                "credits_remaining": 17,
                "reset_at": "2026-09-11T00:00:00Z"
              }
            }
        """.trimIndent()
        val transport = RecordingFunctionsTransport(FunctionsReply(status = 200, body = body))
        val credits = RecordingGenerationCreditsRepository()
        val repository = repository(transport, credits = credits)

        repository.suggest(listOf("hello"))

        assertEquals(listOf(GenerationCredits(remaining = 17, resetAt = RESET_AT)), credits.recorded)
    }

    @Test
    fun `suggest records the exhausted balance carried by a payment required reply`() = runTest {
        val body = """
            {
              "success": false,
              "data": null,
              "error": {
                "code": "credits_exhausted",
                "message": "Te quedaste sin generaciones por hoy.",
                "reset_at": "2026-09-11T00:00:00Z"
              },
              "meta": { "credits_remaining": 0, "reset_at": "2026-09-11T00:00:00Z" }
            }
        """.trimIndent()
        val transport = RecordingFunctionsTransport(FunctionsReply(status = 402, body = body))
        val credits = RecordingGenerationCreditsRepository()
        val repository = repository(transport, credits = credits)

        val error: Throwable? = runCatching { repository.suggest(listOf("hello")) }.exceptionOrNull()

        assertTrue(error is GenerationCreditsExhaustedException)
        assertEquals(listOf(GenerationCredits(remaining = 0, resetAt = RESET_AT)), credits.recorded)
    }

    @Test
    fun `suggest records nothing when the reply carries no credits`() = runTest {
        val body = """
            {
              "situation": "Ordering food at a busy restaurant",
              "words": [{ "word": "the check", "translation": "la cuenta" }],
              "meta": { "cached": false, "provider": "gemini" }
            }
        """.trimIndent()
        val transport = RecordingFunctionsTransport(FunctionsReply(status = 200, body = body))
        val credits = RecordingGenerationCreditsRepository()
        val repository = repository(transport, credits = credits)

        repository.suggest(listOf("hello"))

        assertEquals(emptyList<GenerationCredits>(), credits.recorded)
    }

    @Test
    fun `suggest records nothing when the reply is an error`() = runTest {
        val body = "{\"success\":false,\"error\":{\"code\":\"app_check_rejected\"},\"meta\":null}"
        val transport = RecordingFunctionsTransport(FunctionsReply(status = 401, body = body))
        val credits = RecordingGenerationCreditsRepository()
        val repository = repository(transport, credits = credits)

        runCatching { repository.suggest(listOf("hello")) }

        assertEquals(emptyList<GenerationCredits>(), credits.recorded)
    }

    private fun repository(
        transport: FunctionsTransport,
        session: SessionInitializer = FakeSessionInitializer(),
        credits: GenerationCreditsRepository = RecordingGenerationCreditsRepository(),
    ): RemoteWordSuggestionRepository {
        return RemoteWordSuggestionRepository(
            transport = transport,
            session = session,
            appCheck = FakeAppCheckTokenProvider("app-check-token"),
            telemetry = GenerationTelemetry.NoOp,
            credits = credits,
            json = json,
        )
    }

    private companion object {
        val RESET_AT: Instant = Instant.parse("2026-09-11T00:00:00Z")
    }
}

private class RecordingGenerationCreditsRepository : GenerationCreditsRepository {

    val recorded: MutableList<GenerationCredits> = mutableListOf()

    override fun observe(): Flow<GenerationCredits?> = flowOf(null)

    override suspend fun record(credits: GenerationCredits) {
        recorded += credits
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
        appCheckToken: String,
    ): FunctionsReply {
        this.function = function
        this.body = body
        return reply
    }
}

private class FakeSessionInitializer : SessionInitializer {

    var ensured: Boolean = false
        private set

    override suspend fun ensureSession() {
        ensured = true
    }
}

private class FakeAppCheckTokenProvider(private val appCheckToken: String) : AppCheckTokenProvider {
    override suspend fun token(): String = appCheckToken
}

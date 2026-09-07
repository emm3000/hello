package com.emm.data.flashcard

import com.emm.data.remote.AppCheckTokenProvider
import com.emm.data.remote.FunctionsReply
import com.emm.data.remote.FunctionsTransport
import com.emm.data.remote.SessionInitializer
import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardInputType
import com.emm.domain.generation.AmbiguousGenerationInputException
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.telemetry.GenerationTelemetry
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteFlashcardGenerationRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `generateLearningNote posts the normalized input as a snake case payload`() = runTest {
        val transport = RecordingFunctionsTransport(FunctionsReply(status = 200, body = SUCCESS_BODY))
        val repository = repository(transport)

        repository.generateLearningNote(
            FlashcardGenerationInput(inputType = FlashcardInputType.Word, userText = "  give   up  "),
        )

        val payload: String = transport.body.toString()
        assertEquals("generate-note", transport.function)
        assertTrue(payload, payload.contains("\"input_type\":\"Word\""))
        assertTrue(payload, payload.contains("\"user_text\":\"give up\""))
        assertTrue(payload, payload.contains("\"learning_goal\":\"Both\""))
        assertTrue(payload, payload.contains("\"level_band\":\"A1_A2\""))
        assertTrue(payload, payload.contains("\"register\":\"Neutral\""))
        assertTrue(payload, payload.contains("\"domain\":\"DailyLife\""))
        assertTrue(payload, payload.contains("\"previous_issues\":[]"))
    }

    @Test
    fun `generateLearningNote ensures the session and sends the app check token to the transport`() = runTest {
        val transport = RecordingFunctionsTransport(FunctionsReply(status = 200, body = SUCCESS_BODY))
        val session = FakeSessionInitializer()
        val repository = repository(transport, session)

        repository.generateLearningNote(
            FlashcardGenerationInput(inputType = FlashcardInputType.Word, userText = "give up"),
        )

        assertTrue(session.ensured)
        assertEquals("app-check-token", transport.appCheckToken)
    }

    @Test
    fun `generateLearningNote serialises previous issues as code and field`() = runTest {
        val transport = RecordingFunctionsTransport(FunctionsReply(status = 200, body = SUCCESS_BODY))
        val repository = repository(transport)

        repository.generateLearningNote(
            FlashcardGenerationInput(
                inputType = FlashcardInputType.Word,
                userText = "give up",
                previousIssues = listOf(
                    ValidationIssue.Error(code = IssueCode.MissingUsagePattern, field = "usage_pattern"),
                ),
            ),
        )

        val payload: String = transport.body.toString()
        assertTrue(
            payload,
            payload.contains("\"previous_issues\":[{\"code\":\"missing_usage_pattern\",\"field\":\"usage_pattern\"}]"),
        )
    }

    @Test
    fun `generateLearningNote returns the note when the reply carries a meta object`() = runTest {
        val transport = RecordingFunctionsTransport(FunctionsReply(status = 200, body = SUCCESS_BODY))
        val repository = repository(transport)

        val note: GeneratedLearningNote = repository.generateLearningNote(
            FlashcardGenerationInput(inputType = FlashcardInputType.Word, userText = "pick up"),
        )

        assertEquals("note-1", note.noteId)
        assertEquals("pick up", note.expression.value)
    }

    @Test
    fun `generateLearningNote surfaces a refusal as an ambiguous input error`() = runTest {
        val body = """
            {
              "success": false,
              "data": null,
              "error": { "input": "zzqxwvk", "message": "El texto de entrada no es inteligible." },
              "meta": { "cached": false, "provider": "gemini" }
            }
        """.trimIndent()
        val transport = RecordingFunctionsTransport(FunctionsReply(status = 200, body = body))
        val repository = repository(transport)

        val error: Throwable? = runCatching {
            repository.generateLearningNote(
                FlashcardGenerationInput(inputType = FlashcardInputType.Word, userText = "zzqxwvk"),
            )
        }.exceptionOrNull()

        assertTrue(error is AmbiguousGenerationInputException)
        assertEquals("El texto de entrada no es inteligible.", (error as AmbiguousGenerationInputException).reason)
    }

    private fun repository(
        transport: FunctionsTransport,
        session: SessionInitializer = FakeSessionInitializer(),
    ): RemoteFlashcardGenerationRepository {
        return RemoteFlashcardGenerationRepository(
            transport = transport,
            session = session,
            appCheck = FakeAppCheckTokenProvider("app-check-token"),
            telemetry = GenerationTelemetry.NoOp,
            json = json,
        )
    }

    private companion object {
        val SUCCESS_BODY: String = """
            {
              "success": true,
              "data": {
                "note_id": "note-1",
                "note_type": "phrasal_verb",
                "expression": "pick up",
                "intended_meaning_es": "recoger",
                "simple_definition_en": "to go somewhere and get someone or something",
                "part_of_speech": "phrasal_verb",
                "register": "neutral",
                "level_band": "A1_A2",
                "domain": "daily_life",
                "why_useful": "Sirve para hablar de tareas y movimientos cotidianos.",
                "example_sentence": "I'll pick you up after work.",
                "example_translation": "Te recojo despues del trabajo.",
                "usage_pattern": "pick someone up",
                "cloze_sentence": "I'll ____ you up after work.",
                "cards": [
                  {
                    "card_id": "card-1",
                    "card_type": "recognition",
                    "prompt": "pick up",
                    "expected_answer": "recoger",
                    "evaluation_mode": "flexible_text",
                    "is_active": true
                  },
                  {
                    "card_id": "card-2",
                    "card_type": "production",
                    "prompt": "Como dices 'recoger' en ingles?",
                    "expected_answer": "pick up",
                    "evaluation_mode": "exact",
                    "is_active": true
                  },
                  {
                    "card_id": "card-3",
                    "card_type": "cloze",
                    "prompt": "I'll ____ you up after work.",
                    "expected_answer": "pick",
                    "evaluation_mode": "exact",
                    "is_active": true
                  }
                ],
                "quality_checks": [
                  { "code": "single_meaning", "passed": true, "message": "ok" },
                  { "code": "natural_example", "passed": true, "message": "ok" },
                  { "code": "non_ambiguous_answers", "passed": true, "message": "ok" },
                  { "code": "clear_card_focus", "passed": true, "message": "ok" },
                  { "code": "note_card_alignment", "passed": true, "message": "ok" }
                ]
              },
              "error": null,
              "meta": {
                "cached": false,
                "provider": "gemini",
                "model": "gemini-3.1-flash-lite",
                "prompt_version": 1,
                "schema_version": 1,
                "credits_remaining": 4
              }
            }
        """.trimIndent()
    }
}

private class RecordingFunctionsTransport(
    private val reply: FunctionsReply,
) : FunctionsTransport {

    var function: String = ""
        private set
    var body: JsonElement = JsonNull
        private set
    var appCheckToken: String = ""
        private set

    override suspend fun invoke(
        function: String,
        body: JsonElement,
        appCheckToken: String,
    ): FunctionsReply {
        this.function = function
        this.body = body
        this.appCheckToken = appCheckToken
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

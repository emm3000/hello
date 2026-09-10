package com.emm.data.flashcard

import com.emm.data.remote.AppCheckTokenProvider
import com.emm.data.remote.CreditsMetaReader
import com.emm.data.remote.FunctionsReply
import com.emm.data.remote.FunctionsReplyMapper
import com.emm.data.remote.FunctionsTransport
import com.emm.data.remote.SessionInitializer
import com.emm.data.remote.toRequestDto
import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardGenerationRepository
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GenerationCredits
import com.emm.domain.generation.GenerationCreditsRepository
import com.emm.domain.telemetry.GenerationTelemetry
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

class RemoteFlashcardGenerationRepository(
    private val transport: FunctionsTransport,
    private val session: SessionInitializer,
    private val appCheck: AppCheckTokenProvider,
    private val telemetry: GenerationTelemetry,
    private val credits: GenerationCreditsRepository,
    private val json: Json,
) : FlashcardGenerationRepository {

    override suspend fun generateLearningNote(input: FlashcardGenerationInput): GeneratedLearningNote {
        val reply: FunctionsReply = send(input)
        recordCredits(reply.body)
        return read(reply)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun send(input: FlashcardGenerationInput): FunctionsReply {
        return try {
            session.ensureSession()
            transport.invoke(
                function = FUNCTION_NAME,
                body = json.encodeToJsonElement(input.toRequestDto()),
                appCheckToken = appCheck.token(),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            telemetry.recordCallFailure(kind = KIND, attempts = 1, cause = error)
            throw error
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun read(reply: FunctionsReply): GeneratedLearningNote {
        return try {
            FunctionsReplyMapper.map(reply, json) { body ->
                GeneratedLearningNoteResponseParser.parse(body, json)
            }
        } catch (error: Throwable) {
            recordReadFailure(reply, error)
            throw error
        }
    }

    private suspend fun recordCredits(body: String) {
        val reading: GenerationCredits = CreditsMetaReader.readOrNull(body, json) ?: return
        credits.record(reading)
    }

    private fun recordReadFailure(reply: FunctionsReply, error: Throwable) {
        if (reply.status == HTTP_OK) {
            telemetry.recordParseFailure(
                kind = KIND,
                responseLength = reply.body.length,
                cause = error,
            )
        } else {
            telemetry.recordCallFailure(kind = KIND, attempts = 1, cause = error)
        }
    }

    private companion object {
        const val FUNCTION_NAME: String = "generate-note"
        const val KIND: String = "learning_note"
        const val HTTP_OK: Int = 200
    }
}

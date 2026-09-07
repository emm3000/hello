package com.emm.data.suggestion

import com.emm.data.remote.AppCheckTokenProvider
import com.emm.data.remote.FunctionsReply
import com.emm.data.remote.FunctionsReplyMapper
import com.emm.data.remote.FunctionsTransport
import com.emm.data.remote.SessionInitializer
import com.emm.data.remote.SuggestWordsRequestDto
import com.emm.domain.suggestion.WordSuggestionRepository
import com.emm.domain.suggestion.WordSuggestions
import com.emm.domain.telemetry.GenerationTelemetry
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

class RemoteWordSuggestionRepository(
    private val transport: FunctionsTransport,
    private val session: SessionInitializer,
    private val appCheck: AppCheckTokenProvider,
    private val telemetry: GenerationTelemetry,
    private val json: Json,
) : WordSuggestionRepository {

    override suspend fun suggest(recentWords: List<String>): WordSuggestions {
        val reply: FunctionsReply = send(recentWords)
        return read(reply)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun send(recentWords: List<String>): FunctionsReply {
        return try {
            transport.invoke(
                function = FUNCTION_NAME,
                body = json.encodeToJsonElement(SuggestWordsRequestDto(recentWords = recentWords)),
                accessToken = session.ensureSession(),
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
    private fun read(reply: FunctionsReply): WordSuggestions {
        return try {
            FunctionsReplyMapper.map(reply, json) { body -> WordSuggestionResponseParser.parse(body, json) }
        } catch (error: Throwable) {
            recordReadFailure(reply, error)
            throw error
        }
    }

    private fun recordReadFailure(reply: FunctionsReply, error: Throwable) {
        if (reply.status == HTTP_OK) {
            telemetry.recordParseFailure(
                kind = KIND,
                rawResponse = reply.body.take(MAX_RAW_RESPONSE_CHARS),
                cause = error,
            )
        } else {
            telemetry.recordCallFailure(kind = KIND, attempts = 1, cause = error)
        }
    }

    private companion object {
        const val FUNCTION_NAME: String = "suggest-words"
        const val KIND: String = "word_suggestion"
        const val HTTP_OK: Int = 200
        const val MAX_RAW_RESPONSE_CHARS: Int = 8_000
    }
}

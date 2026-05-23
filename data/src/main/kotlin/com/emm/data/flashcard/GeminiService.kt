package com.emm.data.flashcard

import com.emm.domain.generation.GenerationQuota
import com.emm.domain.generation.GenerationQuotaExceededException
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

open class GeminiService(
    private val generativeModel: GenerativeModel,
    private val learningNoteModel: GenerativeModel = generativeModel,
    private val telemetry: GeminiTelemetry = GeminiTelemetry.NoOp,
    private val quota: GenerationQuota = GenerationQuota.AlwaysAllow,
    private val perAttemptTimeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val backoffMs: List<Long> = DEFAULT_BACKOFF_MS,
) {

    open suspend fun process(prompt: String): String {
        enforceQuota(kind = "generic")
        return callWithRetry(kind = "generic") {
            val response: GenerateContentResponse = generativeModel.generateContent(prompt)
            response.text.orEmpty()
        }
    }

    /**
     * Dedicated call for the main generation of [com.emm.data.flashcard.iadto.GeneratedLearningNoteDto].
     * Uses a model with a declared `responseSchema` to restrict enums and JSON shape.
     */
    open suspend fun processLearningNote(prompt: String): String {
        enforceQuota(kind = "learning_note")
        return callWithRetry(kind = "learning_note") {
            val response: GenerateContentResponse = learningNoteModel.generateContent(prompt)
            response.text.orEmpty()
        }
    }

    /**
     * Like [processLearningNote] but parses the response inside the retry loop:
     * if the model returns a malformed payload (missing required field, wrong type),
     * we retry the whole call instead of surfacing the error to the user.
     */
    open suspend fun <T> processLearningNoteWithParser(
        prompt: String,
        parse: (String) -> T,
    ): T {
        enforceQuota(kind = "learning_note")
        val totalAttempts = backoffMs.size + 1
        var lastError: Throwable? = null
        var lastRaw = ""
        repeat(totalAttempts) { attempt ->
            try {
                val raw = withTimeout(perAttemptTimeoutMs) {
                    learningNoteModel.generateContent(prompt).text.orEmpty()
                }
                lastRaw = raw
                return parse(raw)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                lastError = t
            }
            if (attempt < backoffMs.size) {
                delay(backoffMs[attempt])
            }
        }
        val error = lastError ?: IllegalStateException("Learning note generation failed without throwable")
        if (lastRaw.isNotEmpty()) {
            telemetry.recordParseFailure(
                kind = "learning_note",
                rawResponse = lastRaw.take(MAX_RAW_RESPONSE_CHARS),
                cause = error,
            )
        } else {
            telemetry.recordCallFailure(kind = "learning_note", attempts = totalAttempts, cause = error)
        }
        throw error
    }

    private suspend fun enforceQuota(kind: String) {
        val outcome = quota.tryConsume()
        if (outcome is GenerationQuota.Outcome.Exceeded) {
            telemetry.recordQuotaExceeded(kind = kind, limit = outcome.limit)
            throw GenerationQuotaExceededException(limit = outcome.limit, resetAt = outcome.resetAt)
        }
    }

    private suspend fun callWithRetry(kind: String, block: suspend () -> String): String {
        val totalAttempts = backoffMs.size + 1
        var lastError: Throwable? = null
        repeat(totalAttempts) { attempt ->
            lastError = try {
                return withTimeout(perAttemptTimeoutMs) { block() }
            } catch (timeout: TimeoutCancellationException) {
                timeout
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                t
            }
            if (attempt < backoffMs.size) {
                delay(backoffMs[attempt])
            }
        }
        val error = lastError ?: IllegalStateException("Gemini call failed without throwable")
        telemetry.recordCallFailure(kind = kind, attempts = totalAttempts, cause = error)
        throw error
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS: Long = 15_000L
        const val MAX_RAW_RESPONSE_CHARS: Int = 8_000
        val DEFAULT_BACKOFF_MS: List<Long> = listOf(1_000L, 2_000L, 4_000L)
    }
}

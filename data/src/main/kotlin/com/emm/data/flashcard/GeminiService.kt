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
     * Llamada dedicada a la generación principal de [com.emm.data.flashcard.iadto.GeneratedLearningNoteDto].
     * Usa un modelo con `responseSchema` declarado para restringir enums y forma del JSON.
     */
    open suspend fun processLearningNote(prompt: String): String {
        enforceQuota(kind = "learning_note")
        return callWithRetry(kind = "learning_note") {
            val response: GenerateContentResponse = learningNoteModel.generateContent(prompt)
            response.text.orEmpty()
        }
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
        val DEFAULT_BACKOFF_MS: List<Long> = listOf(1_000L, 2_000L, 4_000L)
    }
}

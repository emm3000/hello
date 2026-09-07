package com.emm.data.flashcard

import com.emm.domain.generation.AppCheckRejectedException
import com.emm.domain.generation.GenerationQuota
import com.emm.domain.generation.GenerationQuotaExceededException
import com.emm.domain.telemetry.GeminiTelemetry
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.UnknownException
import java.io.IOException
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

    open suspend fun <T> processLearningNoteWithParser(
        prompt: String,
        parse: (String) -> T,
    ): T {
        enforceQuota(kind = "learning_note")
        val totalAttempts: Int = backoffMs.size + 1
        var lastError: Throwable = IllegalStateException("Learning note generation failed without throwable")
        for (attempt in 0 until totalAttempts) {
            var raw: String = ""
            try {
                raw = withTimeout(perAttemptTimeoutMs) {
                    learningNoteModel.generateContent(prompt).text.orEmpty()
                }
                return parse(raw)
            } catch (timeout: TimeoutCancellationException) {
                lastError = timeout
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                lastError = t
            }
            if (raw.isNotEmpty()) recordParseFailureAndThrow(raw = raw, error = lastError)
            if (!lastError.isTransient()) {
                recordCallFailureAndThrow(kind = "learning_note", attempts = attempt + 1, error = lastError)
            }
            if (attempt < backoffMs.size) {
                delay(backoffMs[attempt])
            }
        }
        recordCallFailureAndThrow(kind = "learning_note", attempts = totalAttempts, error = lastError)
    }

    private suspend fun enforceQuota(kind: String) {
        val outcome: GenerationQuota.Outcome = quota.tryConsume()
        if (outcome is GenerationQuota.Outcome.Exceeded) {
            telemetry.recordQuotaExceeded(kind = kind, limit = outcome.limit)
            throw GenerationQuotaExceededException(limit = outcome.limit, resetAt = outcome.resetAt)
        }
    }

    private suspend fun callWithRetry(kind: String, block: suspend () -> String): String {
        val totalAttempts: Int = backoffMs.size + 1
        var lastError: Throwable = IllegalStateException("Gemini call failed without throwable")
        for (attempt in 0 until totalAttempts) {
            try {
                return withTimeout(perAttemptTimeoutMs) { block() }
            } catch (timeout: TimeoutCancellationException) {
                lastError = timeout
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                lastError = t
            }
            if (!lastError.isTransient()) {
                recordCallFailureAndThrow(kind = kind, attempts = attempt + 1, error = lastError)
            }
            if (attempt < backoffMs.size) {
                delay(backoffMs[attempt])
            }
        }
        recordCallFailureAndThrow(kind = kind, attempts = totalAttempts, error = lastError)
    }

    private fun recordCallFailureAndThrow(kind: String, attempts: Int, error: Throwable): Nothing {
        telemetry.recordCallFailure(kind = kind, attempts = attempts, cause = error)
        throw error.asDomainError()
    }

    private fun Throwable.asDomainError(): Throwable {
        if (this is ServerException && isAppCheckRejection()) return AppCheckRejectedException(this)
        return this
    }

    private fun recordParseFailureAndThrow(raw: String, error: Throwable): Nothing {
        telemetry.recordParseFailure(
            kind = "learning_note",
            rawResponse = raw.take(MAX_RAW_RESPONSE_CHARS),
            cause = error,
        )
        throw error
    }

    private fun Throwable.isTransient(): Boolean {
        return when (this) {
            is TimeoutCancellationException -> true
            is IOException -> true
            is RequestTimeoutException -> true
            is ServerException -> !isAppCheckRejection()
            is QuotaExceededException -> true
            is UnknownException -> true
            else -> false
        }
    }

    private fun ServerException.isAppCheckRejection(): Boolean {
        return message.orEmpty().contains(APP_CHECK_REJECTION_MARKER, ignoreCase = true)
    }

    private companion object {
        const val APP_CHECK_REJECTION_MARKER: String = "App Check"
        const val DEFAULT_TIMEOUT_MS: Long = 15_000L
        const val MAX_RAW_RESPONSE_CHARS: Int = 8_000
        val DEFAULT_BACKOFF_MS: List<Long> = listOf(1_000L, 2_000L, 4_000L)
    }
}

package com.emm.data.flashcard

import com.emm.domain.generation.GenerationQuota
import com.emm.domain.generation.GenerationQuotaExceededException
import com.emm.domain.telemetry.GeminiTelemetry
import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.UnknownException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeminiServiceRetryTest {

    private val telemetry = mockk<GeminiTelemetry>(relaxed = true)

    @Test
    fun `process succeeds without retry when first call returns text`() = runTest {
        val model = mockk<GenerativeModel>()
        val response = mockk<GenerateContentResponse>()
        every { response.text } returns "ok"
        coEvery { model.generateContent(any<String>()) } returns response

        val service = newService(model)

        val result = service.process("prompt")

        assertEquals("ok", result)
        coVerify(exactly = 1) { model.generateContent(any<String>()) }
        verify(exactly = 0) { telemetry.recordCallFailure(any(), any(), any()) }
    }

    @Test
    fun `process retries on transient error and reports nothing to telemetry on success`() = runTest {
        val model = mockk<GenerativeModel>()
        val response = mockk<GenerateContentResponse>()
        every { response.text } returns "ok"
        coEvery { model.generateContent(any<String>()) } throws IOException("transient") andThenThrows
            IOException("transient") andThen response

        val service = newService(model)

        val result = service.process("prompt")

        assertEquals("ok", result)
        coVerify(exactly = 3) { model.generateContent(any<String>()) }
        verify(exactly = 0) { telemetry.recordCallFailure(any(), any(), any()) }
    }

    @Test
    fun `process records non-fatal and rethrows when all attempts fail`() = runTest {
        val model = mockk<GenerativeModel>()
        val boom = IOException("network down")
        coEvery { model.generateContent(any<String>()) } throws boom

        val service = newService(model)

        val thrown: Throwable = try {
            service.process("prompt")
            error("expected IOException")
        } catch (t: IOException) {
            t
        }
        assertSame(boom, thrown)
        coVerify(exactly = TOTAL_ATTEMPTS) { model.generateContent(any<String>()) }
        verify(exactly = 1) {
            telemetry.recordCallFailure(kind = "generic", attempts = TOTAL_ATTEMPTS, cause = boom)
        }
    }

    @Test
    fun `process throws GenerationQuotaExceededException without calling Gemini when quota is exhausted`() = runTest {
        val model = mockk<GenerativeModel>()
        val resetAt = Instant.parse("2026-05-17T00:00:00Z")
        val quota = object : GenerationQuota {
            override suspend fun tryConsume() = GenerationQuota.Outcome.Exceeded(limit = 50, resetAt = resetAt)
            override fun remainingToday(): Int = 0
        }

        val service = newService(model, quota = quota)

        val thrown: GenerationQuotaExceededException = try {
            service.process("prompt")
            error("expected GenerationQuotaExceededException")
        } catch (t: GenerationQuotaExceededException) {
            t
        }
        assertEquals(50, thrown.limit)
        assertEquals(resetAt, thrown.resetAt)
        coVerify(exactly = 0) { model.generateContent(any<String>()) }
        verify(exactly = 0) { telemetry.recordCallFailure(any(), any(), any()) }
        verify(exactly = 1) { telemetry.recordQuotaExceeded(kind = "generic", limit = 50) }
    }

    @Test
    fun `processLearningNoteWithParser does not retry a parser validation failure`() = runTest {
        val model = mockk<GenerativeModel>()
        val response = mockk<GenerateContentResponse>()
        every { response.text } returns "raw"
        coEvery { model.generateContent(any<String>()) } returns response
        val validationError = DomainValidationException(
            listOf(ValidationIssue.Error(IssueCode.MissingUsagePattern, "usage_pattern")),
        )

        val service = newService(model)

        val thrown: DomainValidationException = try {
            service.processLearningNoteWithParser("prompt") { throw validationError }
            error("expected DomainValidationException")
        } catch (t: DomainValidationException) {
            t
        }

        assertSame(validationError, thrown)
        coVerify(exactly = 1) { model.generateContent(any<String>()) }
        verify(exactly = 1) {
            telemetry.recordParseFailure(kind = "learning_note", rawResponse = "raw", cause = validationError)
        }
        verify(exactly = 0) { telemetry.recordCallFailure(any(), any(), any()) }
    }

    @Test
    fun `processLearningNoteWithParser retries a transient quota error before parsing`() = runTest {
        val model = mockk<GenerativeModel>()
        val response = mockk<GenerateContentResponse>()
        every { response.text } returns "raw"
        val quotaError: QuotaExceededException = firebaseAiException(message = "quota")
        coEvery { model.generateContent(any<String>()) } throws
            quotaError andThenThrows
            firebaseAiException<QuotaExceededException>(message = "quota") andThen response
        val parsed = "parsed-note"

        val service = newService(model)

        val result = service.processLearningNoteWithParser("prompt") { parsed }

        assertEquals(parsed, result)
        coVerify(exactly = 3) { model.generateContent(any<String>()) }
        verify(exactly = 0) { telemetry.recordCallFailure(any(), any(), any()) }
        verify(exactly = 0) { telemetry.recordParseFailure(any(), any(), any()) }
    }

    @Test
    fun `process rethrows a non-transient error immediately and reports the real attempt count`() = runTest {
        val model = mockk<GenerativeModel>()
        val blocked: PromptBlockedException = firebaseAiException(message = "blocked")
        coEvery { model.generateContent(any<String>()) } throws blocked

        val service = newService(model)

        val thrown: PromptBlockedException = try {
            service.process("prompt")
            error("expected PromptBlockedException")
        } catch (t: PromptBlockedException) {
            t
        }

        assertSame(blocked, thrown)
        coVerify(exactly = 1) { model.generateContent(any<String>()) }
        verify(exactly = 1) {
            telemetry.recordCallFailure(kind = "generic", attempts = 1, cause = blocked)
        }
    }

    @Test
    fun `process rethrows an App Check rejection immediately instead of retrying`() = runTest {
        val model = mockk<GenerativeModel>()
        val rejected: ServerException = firebaseAiException(message = "Firebase App Check token is invalid.")
        coEvery { model.generateContent(any<String>()) } throws rejected

        val service = newService(model)

        val thrown: ServerException = try {
            service.process("prompt")
            error("expected ServerException")
        } catch (t: ServerException) {
            t
        }

        assertSame(rejected, thrown)
        coVerify(exactly = 1) { model.generateContent(any<String>()) }
        verify(exactly = 1) {
            telemetry.recordCallFailure(kind = "generic", attempts = 1, cause = rejected)
        }
    }

    @Test
    fun `processLearningNoteWithParser rethrows an App Check rejection immediately instead of retrying`() = runTest {
        val model = mockk<GenerativeModel>()
        val rejected: ServerException = firebaseAiException(message = "Firebase App Check token is invalid.")
        coEvery { model.generateContent(any<String>()) } throws rejected

        val service = newService(model)

        val thrown: ServerException = try {
            service.processLearningNoteWithParser("prompt") { it }
            error("expected ServerException")
        } catch (t: ServerException) {
            t
        }

        assertSame(rejected, thrown)
        coVerify(exactly = 1) { model.generateContent(any<String>()) }
        verify(exactly = 1) {
            telemetry.recordCallFailure(kind = "learning_note", attempts = 1, cause = rejected)
        }
    }

    @Test
    fun `process retries a server error that is not an App Check rejection`() = runTest {
        val model = mockk<GenerativeModel>()
        val response = mockk<GenerateContentResponse>()
        every { response.text } returns "ok"
        val serverError: ServerException = firebaseAiException(message = "Internal error encountered.")
        coEvery { model.generateContent(any<String>()) } throws serverError andThen response

        val service = newService(model)

        val result = service.process("prompt")

        assertEquals("ok", result)
        coVerify(exactly = 2) { model.generateContent(any<String>()) }
        verify(exactly = 0) { telemetry.recordCallFailure(any(), any(), any()) }
    }

    @Test
    fun `process retries a transient unknown error wrapping IOException`() = runTest {
        val model = mockk<GenerativeModel>()
        val response = mockk<GenerateContentResponse>()
        every { response.text } returns "ok"
        val unknown: UnknownException = firebaseAiException(message = "net", cause = IOException("down"))
        coEvery { model.generateContent(any<String>()) } throws unknown andThen response

        val service = newService(model)

        val result = service.process("prompt")

        assertEquals("ok", result)
        coVerify(exactly = 2) { model.generateContent(any<String>()) }
    }

    @Test
    fun `processLearningNoteWithParser retries after a per-attempt timeout`() = runTest {
        val model = mockk<GenerativeModel>()
        val response = mockk<GenerateContentResponse>()
        every { response.text } returns "raw"
        var callCount = 0
        coEvery { model.generateContent(any<String>()) } coAnswers {
            callCount += 1
            if (callCount == 1) delay(5_000L)
            response
        }
        val parsed = "parsed-after-timeout"

        val service = newService(model)

        val result = service.processLearningNoteWithParser("prompt") { parsed }

        assertEquals(parsed, result)
        coVerify(exactly = 2) { model.generateContent(any<String>()) }
    }

    private inline fun <reified T : Throwable> firebaseAiException(message: String, cause: Throwable? = null): T {
        return T::class.java.getConstructor(String::class.java, Throwable::class.java)
            .newInstance(message, cause)
    }

    private fun TestScope.newService(
        model: GenerativeModel,
        quota: GenerationQuota = GenerationQuota.AlwaysAllow,
    ): GeminiService = GeminiService(
        generativeModel = model,
        learningNoteModel = model,
        telemetry = telemetry,
        quota = quota,
        perAttemptTimeoutMs = 1_000L,
        backoffMs = listOf(10L, 20L, 40L),
    )

    private companion object {
        const val TOTAL_ATTEMPTS = 4
    }
}

package com.emm.data.flashcard

import com.emm.domain.generation.GenerationQuota
import com.emm.domain.generation.GenerationQuotaExceededException
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

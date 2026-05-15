package com.emm.hello.newfeatures.card

import java.io.IOException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NewCardErrorClassifierTest {

    @Test
    fun `IOException is classified as network error`() {
        val classified = NewCardErrorClassifier.classifyGenerationFailure(
            error = IOException("connection reset"),
            fallbackMessage = "fallback",
        )
        assertEquals("Sin conexión", classified.title)
    }

    @Test
    fun `TimeoutCancellationException is classified as network error`() {
        val timeout = captureTimeout()
        val classified = NewCardErrorClassifier.classifyGenerationFailure(
            error = timeout,
            fallbackMessage = "fallback",
        )
        assertEquals("Sin conexión", classified.title)
    }

    @Test
    fun `wrapped IOException in cause chain is classified as network error`() {
        val wrapped = RuntimeException("wrapper", IOException("down"))
        val classified = NewCardErrorClassifier.classifyGenerationFailure(
            error = wrapped,
            fallbackMessage = "fallback",
        )
        assertEquals("Sin conexión", classified.title)
    }

    @Test
    fun `parse exception keeps AI title and uses error message`() {
        val classified = NewCardErrorClassifier.classifyGenerationFailure(
            error = IllegalStateException("invalid enum value"),
            fallbackMessage = "fallback",
        )
        assertEquals("Respuesta inválida de IA", classified.title)
        assertEquals("invalid enum value", classified.message)
    }

    @Test
    fun `regeneration network failure overrides custom failureTitle`() {
        val classified = NewCardErrorClassifier.classifyRegenerationFailure(
            error = IOException("eof"),
            failureTitle = "Error al regenerar ejemplo",
            fallbackMessage = "fallback",
        )
        assertEquals("Sin conexión", classified.title)
    }

    @Test
    fun `regeneration non-network failure preserves failureTitle`() {
        val classified = NewCardErrorClassifier.classifyRegenerationFailure(
            error = IllegalArgumentException("bad shape"),
            failureTitle = "Error al regenerar ejemplo",
            fallbackMessage = "fallback",
        )
        assertEquals("Error al regenerar ejemplo", classified.title)
        assertNotEquals("Sin conexión", classified.title)
    }

    private fun captureTimeout(): TimeoutCancellationException = runBlocking {
        try {
            withTimeout(1L) { neverReturns() }
            error("expected TimeoutCancellationException")
        } catch (timeout: TimeoutCancellationException) {
            timeout
        }
    }

    private suspend fun neverReturns(): Nothing {
        kotlinx.coroutines.delay(Long.MAX_VALUE)
        error("unreachable")
    }
}

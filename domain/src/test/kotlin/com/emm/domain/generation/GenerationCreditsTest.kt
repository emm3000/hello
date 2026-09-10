package com.emm.domain.generation

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerationCreditsTest {

    @Test
    fun `isFreshAt is true for two instants on the same UTC day`() {
        val credits = GenerationCredits(remaining = 3, observedAt = Instant.parse("2026-09-10T08:15:00Z"))

        assertTrue(credits.isFreshAt(Instant.parse("2026-09-10T23:59:59Z")))
    }

    @Test
    fun `isFreshAt is false once UTC midnight has passed`() {
        val credits = GenerationCredits(remaining = 3, observedAt = Instant.parse("2026-09-10T23:59:00Z"))

        assertFalse(credits.isFreshAt(Instant.parse("2026-09-11T00:01:00Z")))
    }

    @Test
    fun `isFreshAt is true at the exact start of the observed UTC day`() {
        val credits = GenerationCredits(remaining = 0, observedAt = Instant.parse("2026-09-10T17:42:00Z"))

        assertTrue(credits.isFreshAt(Instant.parse("2026-09-10T00:00:00Z")))
    }

    @Test
    fun `isFreshAt is false for a reading from the previous UTC day`() {
        val credits = GenerationCredits(remaining = 12, observedAt = Instant.parse("2026-09-09T00:00:00Z"))

        assertFalse(credits.isFreshAt(Instant.parse("2026-09-10T00:00:00Z")))
    }

    @Test
    fun `resetsAt is the next UTC midnight after the reading`() {
        val credits = GenerationCredits(remaining = 12, observedAt = Instant.parse("2026-09-10T17:42:00Z"))

        assertEquals(Instant.parse("2026-09-11T00:00:00Z"), credits.resetsAt())
    }

    @Test
    fun `resetsAt on a reading taken at UTC midnight is the following midnight`() {
        val credits = GenerationCredits(remaining = 12, observedAt = Instant.parse("2026-09-10T00:00:00Z"))

        assertEquals(Instant.parse("2026-09-11T00:00:00Z"), credits.resetsAt())
    }

    @Test
    fun `resetsAt is strictly after the reading`() {
        val credits = GenerationCredits(remaining = 12, observedAt = Instant.parse("2026-09-10T23:59:59Z"))

        assertTrue(credits.resetsAt().isAfter(credits.observedAt))
    }

    @Test
    fun `resetsAt crosses the month boundary`() {
        val credits = GenerationCredits(remaining = 1, observedAt = Instant.parse("2026-09-30T22:10:00Z"))

        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), credits.resetsAt())
    }
}

package com.emm.domain.generation

import java.time.Instant
import kotlin.test.Test
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
}

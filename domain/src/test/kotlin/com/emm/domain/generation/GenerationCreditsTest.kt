package com.emm.domain.generation

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerationCreditsTest {

    @Test
    fun `isFreshAt is true one second before the reset the server reported`() {
        val credits = GenerationCredits(remaining = 3, resetAt = Instant.parse("2026-09-11T00:00:00Z"))

        assertTrue(credits.isFreshAt(Instant.parse("2026-09-10T23:59:59Z")))
    }

    @Test
    fun `isFreshAt is false at the exact reset instant`() {
        val credits = GenerationCredits(remaining = 3, resetAt = Instant.parse("2026-09-11T00:00:00Z"))

        assertFalse(credits.isFreshAt(Instant.parse("2026-09-11T00:00:00Z")))
    }

    @Test
    fun `isFreshAt is false once the reset instant has passed`() {
        val credits = GenerationCredits(remaining = 0, resetAt = Instant.parse("2026-09-11T00:00:00Z"))

        assertFalse(credits.isFreshAt(Instant.parse("2026-09-11T00:00:01Z")))
    }

    @Test
    fun `isFreshAt holds for a reading whose reset is days away`() {
        val credits = GenerationCredits(remaining = 12, resetAt = Instant.parse("2026-09-14T00:00:00Z"))

        assertTrue(credits.isFreshAt(Instant.parse("2026-09-11T18:20:00Z")))
    }
}

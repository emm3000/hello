package com.emm.domain.time

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClockTest {

    @Test
    fun `custom clock can return a fixed instant`() {
        val expected = Instant.parse("2026-05-04T12:00:00Z")
        val clock = Clock { expected }

        assertEquals(expected, clock.now())
    }

    @Test
    fun `system clock returns current instant`() {
        val before = Instant.now().minusSeconds(1)

        val current = SystemClock.now()

        assertTrue(current >= before)
    }
}

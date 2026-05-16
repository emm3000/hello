package com.emm.data.flashcard

import android.content.SharedPreferences
import com.emm.domain.generation.GenerationQuota
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyGenerationQuotaTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val day1: Instant = Instant.parse("2026-05-16T10:00:00Z")
    private val day2: Instant = Instant.parse("2026-05-17T10:00:00Z")

    @Test
    fun `tryConsume returns Allowed and increments when count is under limit`() = runTest {
        val storedDate = slot<String>()
        val storedCount = slot<Int>()
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { prefs.getString(DailyGenerationQuota.KEY_DATE, null) } returns "2026-05-16"
        every { prefs.getInt(DailyGenerationQuota.KEY_COUNT, 0) } returns 5
        every { prefs.edit() } returns editor
        every { editor.putString(DailyGenerationQuota.KEY_DATE, capture(storedDate)) } returns editor
        every { editor.putInt(DailyGenerationQuota.KEY_COUNT, capture(storedCount)) } returns editor

        val quota = DailyGenerationQuota(
            preferences = prefs,
            limit = 50,
            zone = zone,
            clock = Clock.fixed(day1, zone),
        )

        val outcome = quota.tryConsume()

        assertEquals(GenerationQuota.Outcome.Allowed, outcome)
        assertEquals("2026-05-16", storedDate.captured)
        assertEquals(6, storedCount.captured)
        verify { editor.apply() }
    }

    @Test
    fun `tryConsume returns Exceeded without writing when count reaches limit`() = runTest {
        val prefs = mockk<SharedPreferences>(relaxed = true)
        every { prefs.getString(DailyGenerationQuota.KEY_DATE, null) } returns "2026-05-16"
        every { prefs.getInt(DailyGenerationQuota.KEY_COUNT, 0) } returns 50

        val quota = DailyGenerationQuota(
            preferences = prefs,
            limit = 50,
            zone = zone,
            clock = Clock.fixed(day1, zone),
        )

        val outcome = quota.tryConsume()

        assertTrue(outcome is GenerationQuota.Outcome.Exceeded)
        outcome as GenerationQuota.Outcome.Exceeded
        assertEquals(50, outcome.limit)
        assertEquals(Instant.parse("2026-05-17T00:00:00Z"), outcome.resetAt)
        verify(exactly = 0) { prefs.edit() }
    }

    @Test
    fun `tryConsume treats stored date from previous day as zero count`() = runTest {
        val storedDate = slot<String>()
        val storedCount = slot<Int>()
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { prefs.getString(DailyGenerationQuota.KEY_DATE, null) } returns "2026-05-16"
        every { prefs.getInt(DailyGenerationQuota.KEY_COUNT, 0) } returns 50
        every { prefs.edit() } returns editor
        every { editor.putString(DailyGenerationQuota.KEY_DATE, capture(storedDate)) } returns editor
        every { editor.putInt(DailyGenerationQuota.KEY_COUNT, capture(storedCount)) } returns editor

        val quota = DailyGenerationQuota(
            preferences = prefs,
            limit = 50,
            zone = zone,
            clock = Clock.fixed(day2, zone),
        )

        val outcome = quota.tryConsume()

        assertEquals(GenerationQuota.Outcome.Allowed, outcome)
        assertEquals("2026-05-17", storedDate.captured)
        assertEquals(1, storedCount.captured)
    }

    @Test
    fun `tryConsume treats missing stored date as fresh day`() = runTest {
        val storedCount = slot<Int>()
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { prefs.getString(DailyGenerationQuota.KEY_DATE, null) } returns null
        every { prefs.getInt(DailyGenerationQuota.KEY_COUNT, 0) } returns 0
        every { prefs.edit() } returns editor
        every { editor.putString(DailyGenerationQuota.KEY_DATE, any()) } returns editor
        every { editor.putInt(DailyGenerationQuota.KEY_COUNT, capture(storedCount)) } returns editor

        val quota = DailyGenerationQuota(
            preferences = prefs,
            limit = 50,
            zone = zone,
            clock = Clock.fixed(day1, zone),
        )

        val outcome = quota.tryConsume()

        assertEquals(GenerationQuota.Outcome.Allowed, outcome)
        assertEquals(1, storedCount.captured)
    }
}

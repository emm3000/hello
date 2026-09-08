package com.emm.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DayRangeTest {

    @Test
    fun `today starts at local midnight and ends at the next local midnight`() {
        val zone: ZoneId = ZoneId.of("America/Lima")
        val now: Instant = LocalDate.of(2026, 5, 4).atTime(15, 59).atZone(zone).toInstant()
        val clock: Clock = Clock { now }

        val range: DayRange = clock.todayRange(zone)

        assertEquals(LocalDate.of(2026, 5, 4).atStartOfDay(zone).toInstant(), range.start)
        assertEquals(LocalDate.of(2026, 5, 5).atStartOfDay(zone).toInstant(), range.endExclusive)
    }

    @Test
    fun `a late evening moment east of UTC still belongs to its own local day`() {
        val zone: ZoneId = ZoneId.of("Asia/Tokyo")
        val now: Instant = LocalDate.of(2026, 5, 4).atTime(23, 30).atZone(zone).toInstant()

        val range: DayRange = Clock { now }.todayRange(zone)

        assertEquals(LocalDate.of(2026, 5, 4).atStartOfDay(zone).toInstant(), range.start)
        assertEquals(LocalDate.of(2026, 5, 5).atStartOfDay(zone).toInstant(), range.endExclusive)
    }

    @Test
    fun `a day that gains an hour still ends at the next local midnight`() {
        val zone: ZoneId = ZoneId.of("America/Santiago")
        val now: Instant = LocalDate.of(2026, 4, 4).atTime(12, 0).atZone(zone).toInstant()

        val range: DayRange = Clock { now }.todayRange(zone)

        assertEquals(LocalDate.of(2026, 4, 5).atStartOfDay(zone).toInstant(), range.endExclusive)
    }
}

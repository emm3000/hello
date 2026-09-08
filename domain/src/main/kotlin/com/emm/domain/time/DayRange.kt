package com.emm.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayRange(val start: Instant, val endExclusive: Instant)

fun Clock.todayRange(zone: ZoneId = ZoneId.systemDefault()): DayRange {
    val today: LocalDate = now().atZone(zone).toLocalDate()
    return DayRange(
        start = today.atStartOfDay(zone).toInstant(),
        endExclusive = today.plusDays(1).atStartOfDay(zone).toInstant(),
    )
}

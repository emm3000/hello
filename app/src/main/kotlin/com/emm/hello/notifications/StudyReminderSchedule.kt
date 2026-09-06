package com.emm.hello.notifications

import java.time.LocalTime
import java.time.ZonedDateTime

internal fun nextOccurrence(target: LocalTime, now: ZonedDateTime): ZonedDateTime {
    val todayAtTarget: ZonedDateTime = now.with(target)
    return if (todayAtTarget.isAfter(now)) todayAtTarget else todayAtTarget.plusDays(1)
}

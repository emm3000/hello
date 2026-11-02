package com.emm.domain.dailywords

import java.time.DayOfWeek
import java.time.LocalTime

data class DailyWordsSettings(
    val isEnabled: Boolean,
    val time: LocalTime,
    val days: Set<DayOfWeek>,
) {
    companion object {
        val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)
        val DEFAULT_DAYS: Set<DayOfWeek> = DayOfWeek.entries.toSet()
        val DEFAULT: DailyWordsSettings = DailyWordsSettings(
            isEnabled = true,
            time = DEFAULT_TIME,
            days = DEFAULT_DAYS,
        )
    }
}

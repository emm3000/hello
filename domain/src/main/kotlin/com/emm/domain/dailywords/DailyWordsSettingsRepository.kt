package com.emm.domain.dailywords

import java.time.DayOfWeek
import java.time.LocalTime

interface DailyWordsSettingsRepository {

    fun get(): DailyWordsSettings

    fun setEnabled(isEnabled: Boolean)

    fun setTime(time: LocalTime)

    fun setDays(days: Set<DayOfWeek>)
}

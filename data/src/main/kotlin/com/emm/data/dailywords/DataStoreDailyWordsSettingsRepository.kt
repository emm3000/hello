package com.emm.data.dailywords

import com.emm.data.remote.DataStore
import com.emm.domain.dailywords.DailyWordsSettings
import com.emm.domain.dailywords.DailyWordsSettingsRepository
import java.time.DayOfWeek
import java.time.LocalTime

class DataStoreDailyWordsSettingsRepository(
    private val dataStore: DataStore,
) : DailyWordsSettingsRepository {

    override fun get(): DailyWordsSettings = DailyWordsSettings(
        isEnabled = dataStore.isDailyWordsEnabled,
        time = LocalTime.of(dataStore.dailyWordsHour, dataStore.dailyWordsMinute),
        days = dataStore.dailyWordsDayNames.mapNotNull { name -> dayOfWeekNamed(name) }.toSet(),
    )

    override fun setEnabled(isEnabled: Boolean) {
        dataStore.isDailyWordsEnabled = isEnabled
    }

    override fun setTime(time: LocalTime) {
        dataStore.dailyWordsHour = time.hour
        dataStore.dailyWordsMinute = time.minute
    }

    override fun setDays(days: Set<DayOfWeek>) {
        dataStore.dailyWordsDayNames = days.map { day -> day.name }.toSet()
    }

    private fun dayOfWeekNamed(name: String): DayOfWeek? =
        DayOfWeek.entries.firstOrNull { day -> day.name == name }
}

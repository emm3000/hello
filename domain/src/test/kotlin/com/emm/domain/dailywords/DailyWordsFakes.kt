package com.emm.domain.dailywords

import java.time.DayOfWeek
import java.time.LocalTime

class FakeDailyWordsSettingsRepository(
    private var settings: DailyWordsSettings,
) : DailyWordsSettingsRepository {

    override fun get(): DailyWordsSettings = settings

    override fun setEnabled(isEnabled: Boolean) {
        settings = settings.copy(isEnabled = isEnabled)
    }

    override fun setTime(time: LocalTime) {
        settings = settings.copy(time = time)
    }

    override fun setDays(days: Set<DayOfWeek>) {
        settings = settings.copy(days = days)
    }
}

class RecordingDailyWordsScheduler : DailyWordsScheduler {

    var scheduledTimes: List<LocalTime> = emptyList()
        private set

    var cancelCount: Int = 0
        private set

    override fun schedule(time: LocalTime) {
        scheduledTimes = scheduledTimes + time
    }

    override fun cancel() {
        cancelCount += 1
    }
}

package com.emm.domain.dailywords

import java.time.DayOfWeek

class ToggleDailyWordsDayUseCase(
    private val repository: DailyWordsSettingsRepository,
    private val syncDailyWords: SyncDailyWordsUseCase,
) {

    operator fun invoke(day: DayOfWeek): Set<DayOfWeek> {
        val days: Set<DayOfWeek> = repository.get().days
        if (days == setOf(day)) return days
        val toggled: Set<DayOfWeek> = if (day in days) days - day else days + day
        repository.setDays(toggled)
        syncDailyWords()
        return toggled
    }
}

package com.emm.domain.dailywords

import java.time.LocalTime

class SetDailyWordsTimeUseCase(
    private val repository: DailyWordsSettingsRepository,
    private val syncDailyWords: SyncDailyWordsUseCase,
) {

    operator fun invoke(time: LocalTime) {
        repository.setTime(time)
        syncDailyWords()
    }
}

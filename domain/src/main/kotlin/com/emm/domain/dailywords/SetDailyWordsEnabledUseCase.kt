package com.emm.domain.dailywords

class SetDailyWordsEnabledUseCase(
    private val repository: DailyWordsSettingsRepository,
    private val syncDailyWords: SyncDailyWordsUseCase,
) {

    operator fun invoke(isEnabled: Boolean) {
        repository.setEnabled(isEnabled)
        syncDailyWords()
    }
}

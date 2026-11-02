package com.emm.domain.dailywords

class SyncDailyWordsUseCase(
    private val repository: DailyWordsSettingsRepository,
    private val scheduler: DailyWordsScheduler,
) {

    operator fun invoke() {
        val settings: DailyWordsSettings = repository.get()
        if (settings.isEnabled && settings.days.isNotEmpty()) {
            scheduler.schedule(settings.time)
        } else {
            scheduler.cancel()
        }
    }
}

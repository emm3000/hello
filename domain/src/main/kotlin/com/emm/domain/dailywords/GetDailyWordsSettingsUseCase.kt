package com.emm.domain.dailywords

class GetDailyWordsSettingsUseCase(
    private val repository: DailyWordsSettingsRepository,
) {

    operator fun invoke(): DailyWordsSettings = repository.get()
}

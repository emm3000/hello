package com.emm.domain.dailywords

import com.emm.domain.suggestion.PrepareDailyWordsUseCase
import com.emm.domain.suggestion.WordSuggestions
import com.emm.domain.time.Clock
import java.time.DayOfWeek
import java.time.ZoneId

class PrepareScheduledDailyWordsUseCase(
    private val settingsRepository: DailyWordsSettingsRepository,
    private val prepareDailyWords: PrepareDailyWordsUseCase,
    private val clock: Clock,
    private val zoneId: ZoneId,
) {

    suspend operator fun invoke(): WordSuggestions? {
        val settings: DailyWordsSettings = settingsRepository.get()
        if (!settings.isEnabled) return null
        val today: DayOfWeek = clock.now().atZone(zoneId).dayOfWeek
        if (today !in settings.days) return null
        return prepareDailyWords()
    }
}

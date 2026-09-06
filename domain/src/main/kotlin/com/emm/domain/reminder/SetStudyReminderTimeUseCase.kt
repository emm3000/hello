package com.emm.domain.reminder

import java.time.LocalTime

class SetStudyReminderTimeUseCase(
    private val repository: StudyReminderSettingsRepository,
    private val syncStudyReminder: SyncStudyReminderUseCase,
) {

    operator fun invoke(time: LocalTime) {
        repository.setTime(time)
        syncStudyReminder()
    }
}

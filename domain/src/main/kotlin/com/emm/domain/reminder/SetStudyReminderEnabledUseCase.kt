package com.emm.domain.reminder

class SetStudyReminderEnabledUseCase(
    private val repository: StudyReminderSettingsRepository,
    private val syncStudyReminder: SyncStudyReminderUseCase,
) {

    operator fun invoke(isEnabled: Boolean) {
        repository.setEnabled(isEnabled)
        syncStudyReminder()
    }
}

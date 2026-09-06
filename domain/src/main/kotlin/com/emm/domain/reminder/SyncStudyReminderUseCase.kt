package com.emm.domain.reminder

class SyncStudyReminderUseCase(
    private val repository: StudyReminderSettingsRepository,
    private val scheduler: StudyReminderScheduler,
) {

    operator fun invoke() {
        val settings: StudyReminderSettings = repository.get()
        if (settings.isEnabled) {
            scheduler.schedule(settings.time)
        } else {
            scheduler.cancel()
        }
    }
}

package com.emm.domain.reminder

class GetStudyReminderSettingsUseCase(
    private val repository: StudyReminderSettingsRepository,
) {

    operator fun invoke(): StudyReminderSettings = repository.get()
}

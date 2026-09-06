package com.emm.domain.reminder

interface StudyReminderSettingsRepository {

    fun get(): StudyReminderSettings

    fun setEnabled(isEnabled: Boolean)
}

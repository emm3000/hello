package com.emm.domain.reminder

import java.time.LocalTime

interface StudyReminderSettingsRepository {

    fun get(): StudyReminderSettings

    fun setEnabled(isEnabled: Boolean)

    fun setTime(time: LocalTime)
}

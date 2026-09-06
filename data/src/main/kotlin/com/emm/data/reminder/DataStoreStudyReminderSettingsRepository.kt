package com.emm.data.reminder

import com.emm.data.remote.DataStore
import com.emm.domain.reminder.StudyReminderSettings
import com.emm.domain.reminder.StudyReminderSettingsRepository
import java.time.LocalTime

class DataStoreStudyReminderSettingsRepository(
    private val dataStore: DataStore,
) : StudyReminderSettingsRepository {

    override fun get(): StudyReminderSettings = StudyReminderSettings(
        isEnabled = dataStore.isStudyReminderEnabled,
        time = LocalTime.of(dataStore.studyReminderHour, dataStore.studyReminderMinute),
    )

    override fun setEnabled(isEnabled: Boolean) {
        dataStore.isStudyReminderEnabled = isEnabled
    }

    override fun setTime(time: LocalTime) {
        dataStore.studyReminderHour = time.hour
        dataStore.studyReminderMinute = time.minute
    }
}

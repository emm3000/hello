package com.emm.data.reminder

import com.emm.data.remote.DataStore
import com.emm.domain.reminder.StudyReminderSettings
import com.emm.domain.reminder.StudyReminderSettingsRepository

class DataStoreStudyReminderSettingsRepository(
    private val dataStore: DataStore,
) : StudyReminderSettingsRepository {

    override fun get(): StudyReminderSettings = StudyReminderSettings(
        isEnabled = dataStore.isStudyReminderEnabled,
        time = StudyReminderSettings.DEFAULT_TIME,
    )

    override fun setEnabled(isEnabled: Boolean) {
        dataStore.isStudyReminderEnabled = isEnabled
    }
}

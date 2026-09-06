package com.emm.domain.reminder

import java.time.LocalTime

class FakeStudyReminderSettingsRepository(
    private var settings: StudyReminderSettings,
) : StudyReminderSettingsRepository {

    var setEnabledCalls: List<Boolean> = emptyList()
        private set

    override fun get(): StudyReminderSettings = settings

    override fun setEnabled(isEnabled: Boolean) {
        settings = settings.copy(isEnabled = isEnabled)
        setEnabledCalls = setEnabledCalls + isEnabled
    }
}

class RecordingStudyReminderScheduler : StudyReminderScheduler {

    var scheduledTimes: List<LocalTime> = emptyList()
        private set

    var cancelCount: Int = 0
        private set

    override fun schedule(time: LocalTime) {
        scheduledTimes = scheduledTimes + time
    }

    override fun cancel() {
        cancelCount += 1
    }
}

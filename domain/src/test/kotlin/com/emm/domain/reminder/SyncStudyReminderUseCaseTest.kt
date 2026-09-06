package com.emm.domain.reminder

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStudyReminderUseCaseTest {

    @Test
    fun `enabled schedules with settings time and never cancels`() {
        val time: LocalTime = LocalTime.of(8, 30)
        val repository = FakeStudyReminderSettingsRepository(StudyReminderSettings(isEnabled = true, time = time))
        val scheduler = RecordingStudyReminderScheduler()
        val useCase = SyncStudyReminderUseCase(repository, scheduler)

        useCase()

        assertEquals(listOf(time), scheduler.scheduledTimes)
        assertEquals(0, scheduler.cancelCount)
    }

    @Test
    fun `disabled cancels and never schedules`() {
        val repository = FakeStudyReminderSettingsRepository(
            StudyReminderSettings(isEnabled = false, time = StudyReminderSettings.DEFAULT_TIME),
        )
        val scheduler = RecordingStudyReminderScheduler()
        val useCase = SyncStudyReminderUseCase(repository, scheduler)

        useCase()

        assertEquals(emptyList<LocalTime>(), scheduler.scheduledTimes)
        assertEquals(1, scheduler.cancelCount)
    }
}

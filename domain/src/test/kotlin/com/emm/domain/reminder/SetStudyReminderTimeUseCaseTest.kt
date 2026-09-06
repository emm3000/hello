package com.emm.domain.reminder

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetStudyReminderTimeUseCaseTest {

    @Test
    fun `enabled persists the new time and schedules once with it`() {
        val time: LocalTime = LocalTime.of(7, 30)
        val repository = FakeStudyReminderSettingsRepository(
            StudyReminderSettings(isEnabled = true, time = StudyReminderSettings.DEFAULT_TIME),
        )
        val scheduler = RecordingStudyReminderScheduler()
        val syncStudyReminder = SyncStudyReminderUseCase(repository, scheduler)
        val useCase = SetStudyReminderTimeUseCase(repository, syncStudyReminder)

        useCase(time)

        assertEquals(listOf(time), repository.setTimeCalls)
        assertEquals(listOf(time), scheduler.scheduledTimes)
        assertEquals(0, scheduler.cancelCount)
        assertEquals(time, repository.get().time)
    }

    @Test
    fun `disabled persists the new time and cancels without scheduling`() {
        val time: LocalTime = LocalTime.of(7, 30)
        val repository = FakeStudyReminderSettingsRepository(
            StudyReminderSettings(isEnabled = false, time = StudyReminderSettings.DEFAULT_TIME),
        )
        val scheduler = RecordingStudyReminderScheduler()
        val syncStudyReminder = SyncStudyReminderUseCase(repository, scheduler)
        val useCase = SetStudyReminderTimeUseCase(repository, syncStudyReminder)

        useCase(time)

        assertEquals(listOf(time), repository.setTimeCalls)
        assertTrue(scheduler.scheduledTimes.isEmpty())
        assertEquals(1, scheduler.cancelCount)
        assertEquals(time, repository.get().time)
    }
}

package com.emm.domain.reminder

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetStudyReminderEnabledUseCaseTest {

    @Test
    fun `disabling persists false and cancels the scheduler`() {
        val time: LocalTime = LocalTime.of(19, 0)
        val repository = FakeStudyReminderSettingsRepository(StudyReminderSettings(isEnabled = true, time = time))
        val scheduler = RecordingStudyReminderScheduler()
        val syncStudyReminder = SyncStudyReminderUseCase(repository, scheduler)
        val useCase = SetStudyReminderEnabledUseCase(repository, syncStudyReminder)

        useCase(false)

        assertEquals(listOf(false), repository.setEnabledCalls)
        assertEquals(1, scheduler.cancelCount)
        assertTrue(scheduler.scheduledTimes.isEmpty())
        assertEquals(false, repository.get().isEnabled)
    }

    @Test
    fun `enabling persists true and schedules with the stored time`() {
        val time: LocalTime = LocalTime.of(19, 0)
        val repository = FakeStudyReminderSettingsRepository(StudyReminderSettings(isEnabled = false, time = time))
        val scheduler = RecordingStudyReminderScheduler()
        val syncStudyReminder = SyncStudyReminderUseCase(repository, scheduler)
        val useCase = SetStudyReminderEnabledUseCase(repository, syncStudyReminder)

        useCase(true)

        assertEquals(listOf(true), repository.setEnabledCalls)
        assertEquals(listOf(time), scheduler.scheduledTimes)
        assertEquals(0, scheduler.cancelCount)
        assertEquals(true, repository.get().isEnabled)
    }
}

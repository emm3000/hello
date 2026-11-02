package com.emm.domain.dailywords

import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncDailyWordsUseCaseTest {

    @Test
    fun `enabled with a selected day schedules the settings time and never cancels`() {
        val time: LocalTime = LocalTime.of(7, 45)
        val scheduler = RecordingDailyWordsScheduler()

        sync(DailyWordsSettings(isEnabled = true, time = time, days = setOf(DayOfWeek.MONDAY)), scheduler)

        assertEquals(listOf(time), scheduler.scheduledTimes)
        assertEquals(0, scheduler.cancelCount)
    }

    @Test
    fun `disabled cancels and never schedules`() {
        val scheduler = RecordingDailyWordsScheduler()

        sync(DailyWordsSettings.DEFAULT.copy(isEnabled = false), scheduler)

        assertEquals(emptyList<LocalTime>(), scheduler.scheduledTimes)
        assertEquals(1, scheduler.cancelCount)
    }

    @Test
    fun `enabled with no selected day cancels and never schedules`() {
        val scheduler = RecordingDailyWordsScheduler()

        sync(DailyWordsSettings.DEFAULT.copy(days = emptySet()), scheduler)

        assertEquals(emptyList<LocalTime>(), scheduler.scheduledTimes)
        assertEquals(1, scheduler.cancelCount)
    }

    private fun sync(settings: DailyWordsSettings, scheduler: RecordingDailyWordsScheduler) {
        SyncDailyWordsUseCase(FakeDailyWordsSettingsRepository(settings), scheduler)()
    }
}

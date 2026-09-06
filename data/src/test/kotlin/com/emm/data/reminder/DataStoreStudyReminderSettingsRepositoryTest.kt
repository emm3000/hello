package com.emm.data.reminder

import android.content.SharedPreferences
import com.emm.data.remote.DataStore
import com.emm.domain.reminder.StudyReminderSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreStudyReminderSettingsRepositoryTest {

    private val editor = mockk<SharedPreferences.Editor>(relaxed = true) {
        every { putBoolean(any(), any()) } returns this
    }
    private val prefs = mockk<SharedPreferences>(relaxed = true) {
        every { edit() } returns editor
    }

    private fun buildRepo() = DataStoreStudyReminderSettingsRepository(DataStore(prefs))

    @Test
    fun `get defaults to enabled with 19_00 when the keys are absent`() {
        every { prefs.getBoolean("STUDY_REMINDER_ENABLED", true) } returns true
        every { prefs.getInt("STUDY_REMINDER_HOUR", 19) } returns 19
        every { prefs.getInt("STUDY_REMINDER_MINUTE", 0) } returns 0

        val repo = buildRepo()
        val settings: StudyReminderSettings = repo.get()

        assertTrue(settings.isEnabled)
        assertEquals(LocalTime.of(19, 0), settings.time)
    }

    @Test
    fun `setEnabled false writes false and applies`() {
        val stored = slot<Boolean>()
        every { editor.putBoolean("STUDY_REMINDER_ENABLED", capture(stored)) } returns editor

        val repo = buildRepo()
        repo.setEnabled(false)

        assertEquals(false, stored.captured)
        verify { editor.apply() }
    }

    @Test
    fun `setTime writes the hour and minute and applies`() {
        val storedHour = slot<Int>()
        val storedMinute = slot<Int>()
        every { editor.putInt("STUDY_REMINDER_HOUR", capture(storedHour)) } returns editor
        every { editor.putInt("STUDY_REMINDER_MINUTE", capture(storedMinute)) } returns editor

        val repo = buildRepo()
        repo.setTime(LocalTime.of(7, 30))

        assertEquals(7, storedHour.captured)
        assertEquals(30, storedMinute.captured)
        verify { editor.apply() }
    }

    @Test
    fun `get reads back a stored 7_30 as LocalTime`() {
        every { prefs.getBoolean("STUDY_REMINDER_ENABLED", true) } returns true
        every { prefs.getInt("STUDY_REMINDER_HOUR", 19) } returns 7
        every { prefs.getInt("STUDY_REMINDER_MINUTE", 0) } returns 30

        val repo = buildRepo()
        val settings: StudyReminderSettings = repo.get()

        assertEquals(LocalTime.of(7, 30), settings.time)
    }
}

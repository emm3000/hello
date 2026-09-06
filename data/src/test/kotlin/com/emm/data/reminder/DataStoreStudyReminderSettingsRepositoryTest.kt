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
    fun `get defaults to enabled with 19_00 when the key is absent`() {
        every { prefs.getBoolean("STUDY_REMINDER_ENABLED", true) } returns true

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
}

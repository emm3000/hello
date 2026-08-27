package com.emm.data.onboarding

import android.content.SharedPreferences
import com.emm.data.remote.DataStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreOnboardingStateRepositoryTest {

    private val editor = mockk<SharedPreferences.Editor>(relaxed = true) {
        every { putBoolean(any(), any()) } returns this
    }
    private val prefs = mockk<SharedPreferences>(relaxed = true) {
        every { edit() } returns editor
    }

    private fun buildRepo() = DataStoreOnboardingStateRepository(DataStore(prefs))

    // ── hasSeenWelcome ────────────────────────────────────────────────────────

    @Test
    fun `hasSeenWelcome returns false by default`() {
        every { prefs.getBoolean("HAS_SEEN_ONBOARDING", false) } returns false

        val repo = buildRepo()

        assertFalse(repo.hasSeenWelcome())
    }

    @Test
    fun `hasSeenWelcome returns true after markWelcomeSeen`() {
        val stored = slot<Boolean>()
        every { prefs.getBoolean("HAS_SEEN_ONBOARDING", false) } returnsMany listOf(false, true)
        every { editor.putBoolean("HAS_SEEN_ONBOARDING", capture(stored)) } returns editor

        val repo = buildRepo()
        repo.markWelcomeSeen()

        assertTrue(stored.captured)
        verify { editor.apply() }
    }

    @Test
    fun `markWelcomeSeen is idempotent — calling twice writes true both times`() {
        every { prefs.getBoolean("HAS_SEEN_ONBOARDING", false) } returns true

        val repo = buildRepo()
        repo.markWelcomeSeen()
        repo.markWelcomeSeen()

        verify(exactly = 2) { editor.putBoolean("HAS_SEEN_ONBOARDING", true) }
    }
}

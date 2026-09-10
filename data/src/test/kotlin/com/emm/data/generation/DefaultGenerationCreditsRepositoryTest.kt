package com.emm.data.generation

import android.content.SharedPreferences
import com.emm.data.remote.DataStore
import com.emm.domain.generation.GenerationCredits
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private const val KEY_REMAINING = "GENERATION_CREDITS_REMAINING"
private const val KEY_RESET_AT = "GENERATION_CREDITS_RESET_AT"

class DefaultGenerationCreditsRepositoryTest {

    private val entries: MutableMap<String, Any> = mutableMapOf()
    private val editor: SharedPreferences.Editor = mockk(relaxed = true)
    private val preferences: SharedPreferences = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { preferences.edit() } returns editor
        every { preferences.contains(any()) } answers { entries.containsKey(firstArg()) }
        every { preferences.getInt(any(), any()) } answers { entries[firstArg<String>()] as? Int ?: secondArg() }
        every { preferences.getLong(any(), any()) } answers { entries[firstArg<String>()] as? Long ?: secondArg() }
        every { editor.putInt(any(), any()) } answers {
            entries[firstArg()] = secondArg<Int>()
            editor
        }
        every { editor.putLong(any(), any()) } answers {
            entries[firstArg()] = secondArg<Long>()
            editor
        }
        every { editor.remove(any()) } answers {
            entries.remove(firstArg<String>())
            editor
        }
    }

    @Test
    fun `a recorded reading is read back by a repository built on the same store`() = runTest {
        val credits = GenerationCredits(remaining = 12, resetAt = Instant.parse("2026-09-11T00:00:00Z"))

        buildRepository().record(credits)

        assertEquals(credits, buildRepository().observe().first())
    }

    @Test
    fun `a recorded reading reaches the observers of the recording repository`() = runTest {
        val credits = GenerationCredits(remaining = 0, resetAt = Instant.parse("2026-09-11T00:00:00Z"))
        val repository = buildRepository()

        repository.record(credits)

        assertEquals(credits, repository.observe().first())
    }

    @Test
    fun `an empty store observes null`() = runTest {
        assertNull(buildRepository().observe().first())
    }

    @Test
    fun `a store holding only the remaining count observes null`() = runTest {
        entries[KEY_REMAINING] = 4

        assertNull(buildRepository().observe().first())
    }

    @Test
    fun `a store holding only the reset instant observes null`() = runTest {
        entries[KEY_RESET_AT] = Instant.parse("2026-09-11T00:00:00Z").toEpochMilli()

        assertNull(buildRepository().observe().first())
    }

    private fun buildRepository(): DefaultGenerationCreditsRepository =
        DefaultGenerationCreditsRepository(DataStore(preferences))
}

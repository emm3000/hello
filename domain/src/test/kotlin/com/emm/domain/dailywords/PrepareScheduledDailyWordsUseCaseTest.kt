package com.emm.domain.dailywords

import com.emm.domain.suggestion.FakeConnectivityRepository
import com.emm.domain.suggestion.FakeFlashcardRepository
import com.emm.domain.suggestion.FakeSuggestionHistoryRepository
import com.emm.domain.suggestion.FakeWordSuggestionCache
import com.emm.domain.suggestion.FakeWordSuggestionRepository
import com.emm.domain.suggestion.ObserveSuggestedWordsUseCase
import com.emm.domain.suggestion.PrepareDailyWordsUseCase
import com.emm.domain.suggestion.RefreshSuggestedWordsUseCase
import com.emm.domain.suggestion.SuggestedWord
import com.emm.domain.suggestion.SuggestedWordFilter
import com.emm.domain.suggestion.WordSuggestions
import com.emm.domain.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrepareScheduledDailyWordsUseCaseTest {

    private val suggestionRepository: FakeWordSuggestionRepository = FakeWordSuggestionRepository(result = FRESH_BATCH)
    private val cache: FakeWordSuggestionCache = FakeWordSuggestionCache()

    @Test
    fun `disabled returns null without fetching`() = runTest {
        val prepared: WordSuggestions? = prepare(DailyWordsSettings.DEFAULT.copy(isEnabled = false))

        assertNull(prepared)
        assertEquals(0, suggestionRepository.calls)
        assertNull(cache.current)
    }

    @Test
    fun `a weekday outside the selected days returns null without fetching`() = runTest {
        val prepared: WordSuggestions? = prepare(DailyWordsSettings.DEFAULT.copy(days = setOf(DayOfWeek.MONDAY)))

        assertNull(prepared)
        assertEquals(0, suggestionRepository.calls)
        assertNull(cache.current)
    }

    @Test
    fun `a selected weekday in the given zone returns the prepared batch`() = runTest {
        val prepared: WordSuggestions? = prepare(DailyWordsSettings.DEFAULT.copy(days = setOf(DayOfWeek.SUNDAY)))

        assertEquals(FRESH_BATCH, prepared)
        assertEquals(1, suggestionRepository.calls)
    }

    private suspend fun prepare(settings: DailyWordsSettings): WordSuggestions? {
        val flashcardRepository = FakeFlashcardRepository(words = emptyList())
        val filter = SuggestedWordFilter(flashcardRepository)
        val clock = Clock { SUNDAY_EVENING_IN_LIMA_MONDAY_IN_UTC }
        val prepareDailyWords = PrepareDailyWordsUseCase(
            observeSuggestedWords = ObserveSuggestedWordsUseCase(filter, cache),
            refreshSuggestedWords = RefreshSuggestedWordsUseCase(
                flashcardRepository = flashcardRepository,
                suggestionRepository = suggestionRepository,
                cache = cache,
                historyRepository = FakeSuggestionHistoryRepository(),
                filter = filter,
                clock = clock,
            ),
            connectivityRepository = FakeConnectivityRepository(online = true),
        )
        val useCase = PrepareScheduledDailyWordsUseCase(
            settingsRepository = FakeDailyWordsSettingsRepository(settings),
            prepareDailyWords = prepareDailyWords,
            clock = clock,
            zoneId = LIMA,
        )
        return useCase()
    }

    private companion object {
        val LIMA: ZoneId = ZoneId.of("America/Lima")
        val SUNDAY_EVENING_IN_LIMA_MONDAY_IN_UTC: Instant = Instant.parse("2026-11-02T03:00:00Z")
        val FRESH_BATCH: WordSuggestions = WordSuggestions("At the airport", listOf(SuggestedWord("gate", "puerta")))
    }
}

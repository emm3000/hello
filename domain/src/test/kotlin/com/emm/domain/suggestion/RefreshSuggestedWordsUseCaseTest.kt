package com.emm.domain.suggestion

import com.emm.domain.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RefreshSuggestedWordsUseCaseTest {

    @Test
    fun `invoke asks the suggestion repository for the recent words`() = runTest {
        val flashcardRepository = FakeFlashcardRepository(words = listOf("hello", "world"))
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(situation = "At a cafe", words = listOf(SuggestedWord("order", "pedir"))),
        )
        val useCase: RefreshSuggestedWordsUseCase = buildUseCase(flashcardRepository, suggestionRepository)

        useCase()

        assertEquals(20, flashcardRepository.receivedLimit)
        assertEquals(listOf("hello", "world"), suggestionRepository.receivedRequest?.recentWords)
    }

    @Test
    fun `invoke stores the filtered batch, preserves the situation and returns what it stored`() = runTest {
        val flashcardRepository = FakeFlashcardRepository(words = listOf("Café"))
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(
                situation = "At a cafe",
                words = listOf(
                    SuggestedWord("cafe", "cafe"),
                    SuggestedWord("order", ""),
                    SuggestedWord("table", "mesa"),
                ),
            ),
        )
        val cache = FakeWordSuggestionCache()
        val useCase: RefreshSuggestedWordsUseCase = buildUseCase(flashcardRepository, suggestionRepository, cache)

        val returned: WordSuggestions = useCase()

        assertEquals(WordSuggestions("At a cafe", listOf(SuggestedWord("table", "mesa"))), cache.current)
        assertEquals(WordSuggestions("At a cafe", listOf(SuggestedWord("table", "mesa"))), returned)
    }

    @Test
    fun `invoke deletes history shown more than 60 days ago and keeps newer history`() = runTest {
        val historyRepository = FakeSuggestionHistoryRepository(
            entries = listOf(
                SuggestionHistoryEntry("At the bank", isSituation = true, shownAt = NOW.minus(Duration.ofDays(61))),
                SuggestionHistoryEntry("At the gym", isSituation = true, shownAt = NOW.minus(Duration.ofDays(59))),
                SuggestionHistoryEntry("loan", isSituation = false, shownAt = NOW.minus(Duration.ofDays(61))),
                SuggestionHistoryEntry("treadmill", isSituation = false, shownAt = NOW.minus(Duration.ofDays(59))),
            ),
        )
        val suggestionRepository = FakeWordSuggestionRepository()
        val useCase: RefreshSuggestedWordsUseCase = buildUseCase(
            suggestionRepository = suggestionRepository,
            historyRepository = historyRepository,
        )

        useCase()

        assertEquals(listOf("At the gym", "treadmill"), historyRepository.entries.map { entry -> entry.text })
        assertEquals(listOf("At the gym"), suggestionRepository.receivedRequest?.excludedSituations)
        assertEquals(listOf("treadmill"), suggestionRepository.receivedRequest?.excludedWords)
    }

    @Test
    fun `invoke excludes the most recent distinct history capped at 30 situations and 120 words`() = runTest {
        val situations: List<SuggestionHistoryEntry> = List(35) { index ->
            SuggestionHistoryEntry("situation $index", isSituation = true, shownAt = minutesAgo(index))
        } + SuggestionHistoryEntry("situation 1", isSituation = true, shownAt = NOW.minusSeconds(30))
        val words: List<SuggestionHistoryEntry> = List(125) { index ->
            SuggestionHistoryEntry("word $index", isSituation = false, shownAt = minutesAgo(index))
        } + SuggestionHistoryEntry("word 1", isSituation = false, shownAt = NOW.minusSeconds(30))
        val suggestionRepository = FakeWordSuggestionRepository()
        val useCase: RefreshSuggestedWordsUseCase = buildUseCase(
            suggestionRepository = suggestionRepository,
            historyRepository = FakeSuggestionHistoryRepository(situations + words),
        )

        useCase()

        val request: WordSuggestionRequest? = suggestionRepository.receivedRequest
        assertEquals(30, request?.excludedSituations?.size)
        assertEquals(listOf("situation 0", "situation 1", "situation 2"), request?.excludedSituations?.take(3))
        assertEquals("situation 29", request?.excludedSituations?.last())
        assertEquals(120, request?.excludedWords?.size)
        assertEquals(listOf("word 0", "word 1", "word 2"), request?.excludedWords?.take(3))
        assertEquals("word 119", request?.excludedWords?.last())
    }

    @Test
    fun `invoke drops a candidate already shown in the history`() = runTest {
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(
                situation = "At a cafe",
                words = listOf(SuggestedWord("order", "pedir"), SuggestedWord("table", "mesa")),
            ),
        )
        val cache = FakeWordSuggestionCache()
        val useCase: RefreshSuggestedWordsUseCase = buildUseCase(
            suggestionRepository = suggestionRepository,
            cache = cache,
            historyRepository = FakeSuggestionHistoryRepository(
                entries = listOf(SuggestionHistoryEntry("Order", isSituation = false, shownAt = minutesAgo(5))),
            ),
        )

        useCase()

        assertEquals(listOf(SuggestedWord("table", "mesa")), cache.current?.words)
    }

    @Test
    fun `invoke leaves the cache untouched when the suggestion repository throws`() = runTest {
        val failure = IllegalStateException("backend unavailable")
        val cache = FakeWordSuggestionCache()
        val useCase: RefreshSuggestedWordsUseCase = buildUseCase(
            suggestionRepository = FakeWordSuggestionRepository(failure = failure),
            cache = cache,
        )

        val thrown: Throwable? = runCatching { useCase() }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertNull(cache.current)
    }

    private fun buildUseCase(
        flashcardRepository: FakeFlashcardRepository = FakeFlashcardRepository(words = emptyList()),
        suggestionRepository: FakeWordSuggestionRepository = FakeWordSuggestionRepository(),
        cache: FakeWordSuggestionCache = FakeWordSuggestionCache(),
        historyRepository: FakeSuggestionHistoryRepository = FakeSuggestionHistoryRepository(),
    ): RefreshSuggestedWordsUseCase = RefreshSuggestedWordsUseCase(
        flashcardRepository = flashcardRepository,
        suggestionRepository = suggestionRepository,
        cache = cache,
        historyRepository = historyRepository,
        filter = SuggestedWordFilter(flashcardRepository),
        clock = Clock { NOW },
    )

    private fun minutesAgo(minutes: Int): Instant = NOW.minus(Duration.ofMinutes(minutes.toLong()))

    private companion object {
        val NOW: Instant = Instant.parse("2026-11-02T12:00:00Z")
    }
}

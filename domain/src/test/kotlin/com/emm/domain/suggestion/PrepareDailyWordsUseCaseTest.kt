package com.emm.domain.suggestion

import com.emm.domain.time.Clock
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrepareDailyWordsUseCaseTest {

    @Test
    fun `an unseen batch with usable items is returned without calling the repository`() = runTest {
        val suggestionRepository = FakeWordSuggestionRepository(result = FRESH_BATCH)
        val useCase: PrepareDailyWordsUseCase = buildUseCase(
            cache = FakeWordSuggestionCache(initial = UNSEEN_BATCH),
            suggestionRepository = suggestionRepository,
        )

        val prepared: WordSuggestions? = useCase()

        assertEquals(WordSuggestions("At a cafe", listOf(SuggestedWord("order", "pedir"))), prepared)
        assertEquals(0, suggestionRepository.calls)
    }

    @Test
    fun `a seen batch while offline returns null without calling the repository`() = runTest {
        assertOfflineReturnsNullWithoutCall(FakeWordSuggestionCache(initial = SEEN_BATCH))
    }

    @Test
    fun `an absent batch while offline returns null without calling the repository`() = runTest {
        assertOfflineReturnsNullWithoutCall(FakeWordSuggestionCache())
    }

    @Test
    fun `an unseen exhausted batch while offline returns null without calling the repository`() = runTest {
        assertOfflineReturnsNullWithoutCall(FakeWordSuggestionCache(initial = UNSEEN_EXHAUSTED_BATCH))
    }

    @Test
    fun `a seen batch while online fetches and returns the new batch`() = runTest {
        assertOnlineFetchesNewBatch(FakeWordSuggestionCache(initial = SEEN_BATCH))
    }

    @Test
    fun `an absent batch while online fetches and returns the new batch`() = runTest {
        assertOnlineFetchesNewBatch(FakeWordSuggestionCache())
    }

    @Test
    fun `an unseen exhausted batch while online fetches and returns the new batch`() = runTest {
        assertOnlineFetchesNewBatch(FakeWordSuggestionCache(initial = UNSEEN_EXHAUSTED_BATCH))
    }

    @Test
    fun `a thrown fetch returns null and leaves the cached batch unchanged`() = runTest {
        val cache = FakeWordSuggestionCache(initial = SEEN_BATCH)
        val useCase: PrepareDailyWordsUseCase = buildUseCase(
            cache = cache,
            suggestionRepository = FakeWordSuggestionRepository(failure = IllegalStateException("backend down")),
        )

        val prepared: WordSuggestions? = useCase()

        assertNull(prepared)
        assertEquals(SEEN_BATCH, cache.current)
    }

    @Test
    fun `a fetched batch with no usable items returns null`() = runTest {
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions("At the airport", listOf(SuggestedWord("order", "pedir"))),
        )
        val useCase: PrepareDailyWordsUseCase = buildUseCase(
            cache = FakeWordSuggestionCache(),
            suggestionRepository = suggestionRepository,
            cardWords = listOf("Order"),
        )

        val prepared: WordSuggestions? = useCase()

        assertNull(prepared)
        assertEquals(1, suggestionRepository.calls)
    }

    private suspend fun assertOfflineReturnsNullWithoutCall(cache: FakeWordSuggestionCache) {
        val suggestionRepository = FakeWordSuggestionRepository(result = FRESH_BATCH)
        val useCase: PrepareDailyWordsUseCase = buildUseCase(
            cache = cache,
            suggestionRepository = suggestionRepository,
            isOnline = false,
        )

        val prepared: WordSuggestions? = useCase()

        assertNull(prepared)
        assertEquals(0, suggestionRepository.calls)
    }

    private suspend fun assertOnlineFetchesNewBatch(cache: FakeWordSuggestionCache) {
        val suggestionRepository = FakeWordSuggestionRepository(result = FRESH_BATCH)
        val useCase: PrepareDailyWordsUseCase = buildUseCase(cache = cache, suggestionRepository = suggestionRepository)

        val prepared: WordSuggestions? = useCase()

        assertEquals(WordSuggestions("At the airport", listOf(SuggestedWord("gate", "puerta"))), prepared)
        assertEquals(1, suggestionRepository.calls)
    }

    private fun buildUseCase(
        cache: FakeWordSuggestionCache,
        suggestionRepository: FakeWordSuggestionRepository,
        isOnline: Boolean = true,
        cardWords: List<String> = listOf("table"),
    ): PrepareDailyWordsUseCase {
        val flashcardRepository = FakeFlashcardRepository(words = cardWords)
        val filter = SuggestedWordFilter(flashcardRepository)
        return PrepareDailyWordsUseCase(
            observeSuggestedWords = ObserveSuggestedWordsUseCase(filter, cache),
            refreshSuggestedWords = RefreshSuggestedWordsUseCase(
                flashcardRepository = flashcardRepository,
                suggestionRepository = suggestionRepository,
                cache = cache,
                historyRepository = FakeSuggestionHistoryRepository(),
                filter = filter,
                clock = Clock { Instant.parse("2026-11-02T12:00:00Z") },
            ),
            connectivityRepository = FakeConnectivityRepository(online = isOnline),
        )
    }

    private companion object {
        val UNSEEN_BATCH: WordSuggestions = WordSuggestions("At a cafe", listOf(SuggestedWord("order", "pedir")))
        val SEEN_BATCH: WordSuggestions = WordSuggestions(
            situation = "At a cafe",
            words = listOf(SuggestedWord("order", "pedir")),
            firstSeenAt = Instant.parse("2026-11-01T09:00:00Z"),
        )
        val UNSEEN_EXHAUSTED_BATCH: WordSuggestions =
            WordSuggestions("At a cafe", listOf(SuggestedWord("table", "mesa")))
        val FRESH_BATCH: WordSuggestions = WordSuggestions("At the airport", listOf(SuggestedWord("gate", "puerta")))
    }
}

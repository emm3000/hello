package com.emm.domain.suggestion

import com.emm.domain.connectivity.ConnectivityRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SuggestedWordsRefresherTest {

    @Test
    fun `ensure does not call the backend when the pool already has words`() = runTest {
        val cache = FakeWordSuggestionCache(initial = WordSuggestions(SITUATION, listOf(ORDER)))
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(this, cache, suggestionRepository)

        refresher.ensure()
        advanceUntilIdle()

        assertEquals(0, suggestionRepository.calls)
        assertEquals(listOf(ORDER), cache.current?.words)
    }

    @Test
    fun `ensure calls the backend once when the cache is empty`() = runTest {
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(this, cache, suggestionRepository)

        refresher.ensure()
        advanceUntilIdle()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(listOf(TABLE), cache.current?.words)
    }

    @Test
    fun `ensure calls the backend once when the cached pool has no usable words`() = runTest {
        val cache = FakeWordSuggestionCache(initial = WordSuggestions(SITUATION, emptyList()))
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(this, cache, suggestionRepository)

        refresher.ensure()
        advanceUntilIdle()

        assertEquals(1, suggestionRepository.calls)
    }

    @Test
    fun `two overlapping ensure calls produce exactly one backend call`() = runTest {
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(this, cache, suggestionRepository)

        refresher.ensure()
        refresher.ensure()
        advanceUntilIdle()

        assertEquals(1, suggestionRepository.calls)
    }

    @Test
    fun `ensure while offline reports offline and does not call the backend`() = runTest {
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(
            scope = this,
            cache = cache,
            suggestionRepository = suggestionRepository,
            connectivityRepository = FakeConnectivityRepository(online = false),
        )

        refresher.ensure()
        advanceUntilIdle()

        assertEquals(0, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Offline, refresher.status.value)
    }

    @Test
    fun `a failing backend moves the status from running to failed`() = runTest {
        val failure = IllegalStateException("backend unavailable")
        val gate = CompletableDeferred<Unit>()
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(failure = failure, gate = gate)
        val refresher: SuggestedWordsRefresher = buildRefresher(this, cache, suggestionRepository)

        refresher.ensure()
        advanceUntilIdle()
        assertEquals(SuggestionRefreshStatus.Running, refresher.status.value)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(SuggestionRefreshStatus.Failed(failure), refresher.status.value)
        assertNull(cache.current)
    }

    @Test
    fun `refresh calls the backend even when the pool already has words`() = runTest {
        val cache = FakeWordSuggestionCache(initial = WordSuggestions(SITUATION, listOf(ORDER)))
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(this, cache, suggestionRepository)

        refresher.refresh()
        advanceUntilIdle()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(listOf(TABLE), cache.current?.words)
    }

    @Test
    fun `two refresh calls before advancing make exactly one backend call`() = runTest {
        val cache = FakeWordSuggestionCache(initial = WordSuggestions(SITUATION, listOf(ORDER)))
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(this, cache, suggestionRepository)

        refresher.refresh()
        refresher.refresh()
        advanceUntilIdle()

        assertEquals(1, suggestionRepository.calls)
    }

    @Test
    fun `refresh while offline reports running then offline`() = runTest {
        val cache = FakeWordSuggestionCache(initial = WordSuggestions(SITUATION, listOf(ORDER)))
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(
            scope = this,
            cache = cache,
            suggestionRepository = suggestionRepository,
            connectivityRepository = FakeConnectivityRepository(online = false),
        )

        refresher.refresh()
        assertEquals(SuggestionRefreshStatus.Running, refresher.status.value)

        advanceUntilIdle()

        assertEquals(SuggestionRefreshStatus.Offline, refresher.status.value)
        assertEquals(0, suggestionRepository.calls)
    }

    @Test
    fun `refresh while a refresh is running is ignored`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(SITUATION, listOf(TABLE)),
            gate = gate,
        )
        val refresher: SuggestedWordsRefresher = buildRefresher(this, cache, suggestionRepository)

        refresher.refresh()
        advanceUntilIdle()
        assertEquals(SuggestionRefreshStatus.Running, refresher.status.value)

        refresher.refresh()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, suggestionRepository.calls)
    }

    @Test
    fun `the status reports completed after a successful refresh`() = runTest {
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(this, cache, suggestionRepository)

        refresher.ensure()
        advanceUntilIdle()

        assertTrue(refresher.status.value is SuggestionRefreshStatus.Completed)
    }

    private fun buildRefresher(
        scope: CoroutineScope,
        cache: FakeWordSuggestionCache,
        suggestionRepository: FakeWordSuggestionRepository,
        connectivityRepository: ConnectivityRepository = FakeConnectivityRepository(),
        flashcardRepository: FakeFlashcardRepository = FakeFlashcardRepository(recentWords = emptyList()),
    ): SuggestedWordsRefresher = SuggestedWordsRefresher(
        observeSuggestedWords = ObserveSuggestedWordsUseCase(flashcardRepository, cache),
        refreshSuggestedWords = RefreshSuggestedWordsUseCase(flashcardRepository, suggestionRepository, cache),
        connectivityRepository = connectivityRepository,
        scope = scope,
    )

    private companion object {
        const val SITUATION: String = "At a cafe"
        val ORDER: SuggestedWord = SuggestedWord("order", "pedir")
        val TABLE: SuggestedWord = SuggestedWord("table", "mesa")
    }
}

package com.emm.domain.suggestion

import com.emm.domain.connectivity.ConnectivityRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
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
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.ensure()
        runCurrent()

        assertEquals(0, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Idle, refresher.status.value)
        assertEquals(listOf(ORDER), cache.current?.words)
    }

    @Test
    fun `ensure calls the backend once when the cache is empty`() = runTest {
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.ensure()
        runCurrent()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Completed, refresher.status.value)
        assertEquals(listOf(TABLE), cache.current?.words)
    }

    @Test
    fun `ensure calls the backend once when the cached pool has no usable words`() = runTest {
        val cache = FakeWordSuggestionCache(initial = WordSuggestions(SITUATION, emptyList()))
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.ensure()
        runCurrent()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Completed, refresher.status.value)
    }

    @Test
    fun `two ensure calls before advancing produce exactly one backend call`() = runTest {
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.ensure()
        refresher.ensure()
        runCurrent()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Completed, refresher.status.value)
    }

    @Test
    fun `ensure while offline reports offline and does not call the backend`() = runTest {
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(
            scope = backgroundScope,
            cache = cache,
            suggestionRepository = suggestionRepository,
            connectivityRepository = FakeConnectivityRepository(online = false),
        )

        refresher.ensure()
        runCurrent()

        assertEquals(0, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Offline, refresher.status.value)
    }

    @Test
    fun `a failing backend moves the status from running to failed`() = runTest {
        val failure = IllegalStateException("backend unavailable")
        val gate = CompletableDeferred<Unit>()
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(failure = failure, gate = gate)
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.ensure()
        runCurrent()
        assertEquals(SuggestionRefreshStatus.Running, refresher.status.value)

        gate.complete(Unit)
        runCurrent()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Failed(failure), refresher.status.value)
        assertNull(cache.current)
    }

    @Test
    fun `refresh calls the backend even when the pool already has words`() = runTest {
        val cache = FakeWordSuggestionCache(initial = WordSuggestions(SITUATION, listOf(ORDER)))
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.refresh()
        runCurrent()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Completed, refresher.status.value)
        assertEquals(listOf(TABLE), cache.current?.words)
    }

    @Test
    fun `two refresh calls before advancing make exactly one backend call`() = runTest {
        val cache = FakeWordSuggestionCache(initial = WordSuggestions(SITUATION, listOf(ORDER)))
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.refresh()
        refresher.refresh()
        runCurrent()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Completed, refresher.status.value)
    }

    @Test
    fun `a refresh asked while a fetch is in flight is satisfied by that fetch`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(SITUATION, listOf(TABLE)),
            gate = gate,
        )
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.refresh()
        runCurrent()
        assertEquals(SuggestionRefreshStatus.Running, refresher.status.value)

        refresher.refresh()
        runCurrent()
        gate.complete(Unit)
        runCurrent()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Completed, refresher.status.value)
    }

    @Test
    fun `refresh while offline reports offline and does not call the backend`() = runTest {
        val cache = FakeWordSuggestionCache(initial = WordSuggestions(SITUATION, listOf(ORDER)))
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(
            scope = backgroundScope,
            cache = cache,
            suggestionRepository = suggestionRepository,
            connectivityRepository = FakeConnectivityRepository(online = false),
        )

        refresher.refresh()
        runCurrent()

        assertEquals(0, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Offline, refresher.status.value)
    }

    @Test
    fun `ensure then refresh before advancing with an empty cache make exactly one backend call`() = runTest {
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.ensure()
        refresher.refresh()
        runCurrent()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Completed, refresher.status.value)
    }

    @Test
    fun `ensure then refresh before advancing with a cached pool make exactly one backend call`() = runTest {
        val cache = FakeWordSuggestionCache(initial = WordSuggestions(SITUATION, listOf(ORDER)))
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.ensure()
        refresher.refresh()
        runCurrent()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Completed, refresher.status.value)
    }

    @Test
    fun `a throwing connectivity read reports failed and leaves the refresher usable`() = runTest {
        val failure = IllegalStateException("connectivity unavailable")
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val connectivityRepository = FakeConnectivityRepository()
        connectivityRepository.failNextObserveWith(failure)
        val refresher: SuggestedWordsRefresher = buildRefresher(
            scope = backgroundScope,
            cache = cache,
            suggestionRepository = suggestionRepository,
            connectivityRepository = connectivityRepository,
        )

        refresher.refresh()
        runCurrent()

        assertEquals(0, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Failed(failure), refresher.status.value)

        connectivityRepository.clearFailure()
        connectivityRepository.setOnline(true)
        refresher.ensure()
        runCurrent()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Completed, refresher.status.value)
    }

    @Test
    fun `a throwing pool read reports failed instead of idle`() = runTest {
        val failure = IllegalStateException("cache unavailable")
        val cache = FakeWordSuggestionCache()
        cache.failNextObserveWith(failure)
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.ensure()
        runCurrent()

        assertEquals(0, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Failed(failure), refresher.status.value)
    }

    @Test
    fun `a refresh issued after a completed fetch asks the backend again`() = runTest {
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.refresh()
        runCurrent()
        refresher.refresh()
        runCurrent()

        assertEquals(2, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Completed, refresher.status.value)
    }

    @Test
    fun `a refresh queued behind a failing ensure is answered by that failure`() = runTest {
        val failure = IllegalStateException("backend unavailable")
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(failure = failure)
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.ensure()
        refresher.refresh()
        runCurrent()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Failed(failure), refresher.status.value)
    }

    @Test
    fun `a refresh queued behind an ensure that fetches nothing is not lost`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val cache = FakeWordSuggestionCache(initial = WordSuggestions(SITUATION, listOf(ORDER)))
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(
            scope = backgroundScope,
            cache = cache,
            suggestionRepository = suggestionRepository,
            flashcardRepository = FakeFlashcardRepository(recentWords = emptyList(), recentWordsGate = gate),
        )

        refresher.ensure()
        refresher.refresh()
        runCurrent()

        gate.complete(Unit)
        runCurrent()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Completed, refresher.status.value)
    }

    @Test
    fun `a cancellation raised by the backend reports failed and keeps the actor alive`() = runTest {
        val cancellation = CancellationException("backend timed out")
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(SITUATION, listOf(TABLE)),
            failure = cancellation,
        )
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)

        refresher.ensure()
        runCurrent()

        assertEquals(SuggestionRefreshStatus.Failed(cancellation), refresher.status.value)

        suggestionRepository.clearFailure()
        refresher.ensure()
        runCurrent()

        assertEquals(2, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Completed, refresher.status.value)
    }

    @Test
    fun `a request issued after a terminal status is published is processed`() = runTest {
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(result = WordSuggestions(SITUATION, listOf(TABLE)))
        val refresher: SuggestedWordsRefresher = buildRefresher(backgroundScope, cache, suggestionRepository)
        var reacted: Boolean = false
        backgroundScope.launch {
            refresher.status.collect { current: SuggestionRefreshStatus ->
                if (current == SuggestionRefreshStatus.Completed && !reacted) {
                    reacted = true
                    refresher.refresh()
                }
            }
        }
        runCurrent()

        refresher.refresh()
        runCurrent()

        assertTrue(reacted)
        assertEquals(2, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Completed, refresher.status.value)
    }

    @Test
    fun `cancelling the scope stops the actor without reporting failed`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val cache = FakeWordSuggestionCache()
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(SITUATION, listOf(TABLE)),
            gate = gate,
        )
        val actorScope: CoroutineScope = CoroutineScope(backgroundScope.coroutineContext + Job())
        val refresher: SuggestedWordsRefresher = buildRefresher(actorScope, cache, suggestionRepository)

        refresher.refresh()
        runCurrent()
        assertEquals(SuggestionRefreshStatus.Running, refresher.status.value)

        actorScope.cancel()
        runCurrent()

        assertEquals(1, suggestionRepository.calls)
        assertEquals(SuggestionRefreshStatus.Running, refresher.status.value)
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

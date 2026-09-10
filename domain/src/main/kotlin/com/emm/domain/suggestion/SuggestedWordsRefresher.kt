package com.emm.domain.suggestion

import com.emm.domain.connectivity.ConnectivityRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SuggestedWordsRefresher(
    private val observeSuggestedWords: ObserveSuggestedWordsUseCase,
    private val refreshSuggestedWords: RefreshSuggestedWordsUseCase,
    private val connectivityRepository: ConnectivityRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val refreshMutex = Mutex()
    private val mutableStatus = MutableStateFlow<SuggestionRefreshStatus>(SuggestionRefreshStatus.Idle)

    val status: StateFlow<SuggestionRefreshStatus> = mutableStatus.asStateFlow()

    fun ensure() {
        if (isRunning()) return
        scope.launch {
            refreshMutex.withLock {
                val pool: WordSuggestions? = observeSuggestedWords().first()
                if (pool != null && pool.words.isNotEmpty()) return@withLock
                fetchNewBatch()
            }
        }
    }

    fun refresh() {
        if (isRunning()) return
        mutableStatus.value = SuggestionRefreshStatus.Running
        scope.launch {
            refreshMutex.withLock { fetchNewBatch() }
        }
    }

    private fun isRunning(): Boolean = mutableStatus.value is SuggestionRefreshStatus.Running

    private suspend fun fetchNewBatch() {
        if (!connectivityRepository.observeOnline().first()) {
            mutableStatus.value = SuggestionRefreshStatus.Offline
            return
        }
        mutableStatus.value = SuggestionRefreshStatus.Running
        try {
            refreshSuggestedWords()
            mutableStatus.value = SuggestionRefreshStatus.Completed
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            mutableStatus.value = SuggestionRefreshStatus.Failed(error)
        }
    }
}

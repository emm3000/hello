package com.emm.domain.suggestion

import com.emm.domain.connectivity.ConnectivityRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SuggestedWordsRefresher(
    private val observeSuggestedWords: ObserveSuggestedWordsUseCase,
    private val refreshSuggestedWords: RefreshSuggestedWordsUseCase,
    private val connectivityRepository: ConnectivityRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private sealed interface Request {
        data object Ensure : Request
        data object Force : Request
    }

    private val requests: Channel<Request> = Channel(Channel.UNLIMITED)
    private val mutableStatus = MutableStateFlow<SuggestionRefreshStatus>(SuggestionRefreshStatus.Idle)

    val status: StateFlow<SuggestionRefreshStatus> = mutableStatus.asStateFlow()

    init {
        scope.launch {
            for (request in requests) {
                val outcome: SuggestionRefreshStatus? = attempt(request)
                if (outcome != null) {
                    discardPendingRequests()
                    mutableStatus.value = outcome
                }
            }
        }
    }

    fun ensure() {
        requests.trySend(Request.Ensure)
    }

    fun refresh() {
        requests.trySend(Request.Force)
    }

    private suspend fun attempt(request: Request): SuggestionRefreshStatus? = try {
        when (request) {
            Request.Ensure -> if (hasUsablePool()) null else fetchNewBatch()
            Request.Force -> fetchNewBatch()
        }
    } catch (cancellation: CancellationException) {
        currentCoroutineContext().ensureActive()
        SuggestionRefreshStatus.Failed(cancellation)
    } catch (error: Throwable) {
        SuggestionRefreshStatus.Failed(error)
    }

    private suspend fun hasUsablePool(): Boolean {
        val pool: WordSuggestions? = observeSuggestedWords().first()
        return pool != null && pool.words.isNotEmpty()
    }

    private suspend fun fetchNewBatch(): SuggestionRefreshStatus {
        if (!connectivityRepository.observeOnline().first()) return SuggestionRefreshStatus.Offline
        mutableStatus.value = SuggestionRefreshStatus.Running
        refreshSuggestedWords()
        return SuggestionRefreshStatus.Completed
    }

    private fun discardPendingRequests() {
        var pending: ChannelResult<Request> = requests.tryReceive()
        while (pending.isSuccess) {
            pending = requests.tryReceive()
        }
    }
}

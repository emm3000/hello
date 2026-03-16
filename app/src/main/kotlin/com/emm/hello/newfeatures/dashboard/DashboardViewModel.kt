package com.emm.hello.newfeatures.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.sync.GetSyncDebugStateUseCase
import com.emm.domain.sync.SyncEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    getDecksUseCase: GetDecksUseCase,
    getSyncDebugStateUseCase: GetSyncDebugStateUseCase,
    private val syncEngine: SyncEngine,
) : ViewModel() {

    private val effectChannel = Channel<DashboardUiEffect>(Channel.BUFFERED)
    val effect: Flow<DashboardUiEffect> = effectChannel.receiveAsFlow()

    private val syncDebugState = combine(
        getSyncDebugStateUseCase(),
        syncEngine.state,
    ) { syncDebug, syncState ->
        SyncDebugUiState(
            pendingOperations = syncDebug.pendingOperations,
            isSyncRunning = syncState.isRunning,
            lastSuccessfulSyncAt = syncDebug.lastSuccessfulSyncAt ?: syncState.lastSuccessfulSyncAt,
            lastSyncError = syncDebug.lastSyncError ?: syncState.lastSyncError,
            deviceId = syncDebug.deviceId,
            appAccountId = syncDebug.appAccountId,
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        getDecksUseCase(),
        syncDebugState,
    ) { decks, syncDebug ->
        DashboardUiState(
            decks = decks,
            isLoading = false,
            syncDebug = syncDebug,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true),
    )

    fun onIntent(intent: DashboardUiIntent) {
        when (intent) {
            DashboardUiIntent.RefreshSync -> refreshSync()
        }
    }

    private fun refreshSync() {
        viewModelScope.launch {
            runCatching { syncEngine.runOnce() }
                .onFailure {
                    effectChannel.send(DashboardUiEffect.SyncFailed(it.message ?: "sync_failed"))
                }
        }
    }
}

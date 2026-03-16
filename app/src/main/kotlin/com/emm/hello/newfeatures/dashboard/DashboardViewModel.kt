package com.emm.hello.newfeatures.dashboard

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.sync.GetSyncDebugStateUseCase
import com.emm.domain.sync.SyncEngine
import com.emm.hello.core.mvi.MviViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DashboardViewModel(
    getDecksUseCase: GetDecksUseCase,
    getSyncDebugStateUseCase: GetSyncDebugStateUseCase,
    private val syncEngine: SyncEngine,
) : MviViewModel<DashboardUiState, DashboardUiIntent, DashboardUiEffect>(
    initialState = DashboardUiState(isLoading = true),
) {

    init {
        combine(
            getDecksUseCase(),
            getSyncDebugStateUseCase(),
            syncEngine.state,
        ) { decks, syncDebug, syncState ->
            mutableState.value.copy(
                decks = decks,
                isLoading = false,
                syncDebug = SyncDebugUiState(
                    pendingOperations = syncDebug.pendingOperations,
                    isSyncRunning = syncState.isRunning,
                    lastSuccessfulSyncAt = syncDebug.lastSuccessfulSyncAt ?: syncState.lastSuccessfulSyncAt,
                    lastSyncError = syncDebug.lastSyncError ?: syncState.lastSyncError,
                    deviceId = syncDebug.deviceId,
                    appAccountId = syncDebug.appAccountId,
                ),
            )
        }.onEach { mutableState.value = it }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: DashboardUiIntent) {
        when (intent) {
            DashboardUiIntent.RefreshSync -> refreshSync()
        }
    }

    private fun refreshSync() {
        viewModelScope.launch {
            runCatching { syncEngine.runOnce() }
                .onFailure {
                    mutableEffect.send(DashboardUiEffect.SyncFailed(it.message ?: "sync_failed"))
                }
        }
    }
}

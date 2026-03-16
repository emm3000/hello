package com.emm.hello.newfeatures.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.sync.GetSyncDebugStateUseCase
import com.emm.domain.sync.SyncEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    getDecksUseCase: GetDecksUseCase,
    getSyncDebugStateUseCase: GetSyncDebugStateUseCase,
    syncEngine: SyncEngine,
) : ViewModel() {

    private val syncDebugState = combine(
        getSyncDebugStateUseCase.fetch(),
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

    val state: StateFlow<DashboardUiState> = combine(
        getDecksUseCase.fetch(),
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
}

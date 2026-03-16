package com.emm.hello.newfeatures.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.HelloDb
import com.emm.domain.deck.DeckFetcher
import com.emm.domain.sync.SyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    deckFetcher: DeckFetcher,
    db: HelloDb,
    syncEngine: SyncEngine,
) : ViewModel() {

    private val syncDebugState = combine(
        db.localFirstQueries.countPendingOperations().asFlow().mapToOne(Dispatchers.IO),
        db.localFirstQueries.selectSyncCheckpoint().asFlow().mapToOneOrNull(Dispatchers.IO),
        db.localFirstQueries.selectLocalDeviceIdentity().asFlow().mapToOneOrNull(Dispatchers.IO),
        db.localFirstQueries.selectLocalAccountState().asFlow().mapToOneOrNull(Dispatchers.IO),
        syncEngine.state,
    ) { pendingOps, checkpoint, deviceIdentity, accountState, syncState ->
        SyncDebugUiState(
            pendingOperations = pendingOps,
            isSyncRunning = syncState.isRunning,
            lastSuccessfulSyncAt = checkpoint?.lastSuccessfulSyncAt ?: syncState.lastSuccessfulSyncAt,
            lastSyncError = checkpoint?.lastSyncError ?: syncState.lastSyncError,
            deviceId = deviceIdentity?.deviceId,
            appAccountId = accountState?.appAccountId,
        )
    }

    val state: StateFlow<DashboardUiState> = combine(
        deckFetcher.fetch(),
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

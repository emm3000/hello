package com.emm.hello.newfeatures.dashboard

import com.emm.domain.deck.Deck

data class DashboardUiState(
    val decks: List<Deck> = emptyList(),
    val isLoading: Boolean = true,
    val syncDebug: SyncDebugUiState = SyncDebugUiState(),
)

data class SyncDebugUiState(
    val pendingOperations: Long = 0L,
    val isSyncRunning: Boolean = false,
    val lastSuccessfulSyncAt: Long? = null,
    val lastSyncError: String? = null,
    val deviceId: String? = null,
    val appAccountId: String? = null,
    val modeLabel: String = "remote",
    val remoteAvailable: Boolean = true,
)

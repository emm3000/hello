package com.emm.domain.sync

data class SyncState(
    val isRunning: Boolean = false,
    val pendingOperations: Long = 0L,
    val lastSuccessfulSyncAt: Long? = null,
    val lastSyncError: String? = null,
)

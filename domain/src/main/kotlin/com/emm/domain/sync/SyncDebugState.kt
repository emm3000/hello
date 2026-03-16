package com.emm.domain.sync

data class SyncDebugState(
    val pendingOperations: Long = 0L,
    val lastSuccessfulSyncAt: Long? = null,
    val lastSyncError: String? = null,
    val deviceId: String? = null,
    val appAccountId: String? = null,
)

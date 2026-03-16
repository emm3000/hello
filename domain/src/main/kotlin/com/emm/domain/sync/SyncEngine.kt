package com.emm.domain.sync

import kotlinx.coroutines.flow.StateFlow

interface SyncEngine {
    val state: StateFlow<SyncState>
    suspend fun runOnce()
}

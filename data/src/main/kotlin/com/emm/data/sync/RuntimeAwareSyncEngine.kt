package com.emm.data.sync

import com.emm.domain.sync.SyncEngine
import com.emm.domain.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RuntimeAwareSyncEngine(
    private val delegate: DefaultSyncEngine,
    private val syncRuntimePolicy: SyncRuntimePolicy,
) : SyncEngine {

    private val localOnlyState = MutableStateFlow(SyncState())

    override val state: StateFlow<SyncState> = if (syncRuntimePolicy.remoteEnabled) {
        delegate.state
    } else {
        localOnlyState.asStateFlow()
    }

    override suspend fun runOnce() {
        if (!syncRuntimePolicy.remoteEnabled) {
            localOnlyState.value = SyncState()
            return
        }
        delegate.runOnce()
    }
}

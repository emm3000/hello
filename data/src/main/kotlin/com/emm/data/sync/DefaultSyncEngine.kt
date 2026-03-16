package com.emm.data.sync

import com.emm.data.HelloDb
import com.emm.domain.sync.SyncEngine
import com.emm.domain.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

class DefaultSyncEngine(
    db: HelloDb,
) : SyncEngine {

    private val localFirstQueries = db.localFirstQueries
    private val mutableState = MutableStateFlow(SyncState())

    override val state: StateFlow<SyncState> = mutableState.asStateFlow()

    override suspend fun runOnce() {
        val currentPending = localFirstQueries.countPendingOperations().executeAsOne()
        mutableState.value = mutableState.value.copy(
            isRunning = true,
            pendingOperations = currentPending,
            lastSyncError = null,
        )

        // Fase 2: unica abstraccion de SyncEngine lista, sin push/pull remoto todavia.
        mutableState.value = mutableState.value.copy(
            isRunning = false,
            pendingOperations = currentPending,
            lastSuccessfulSyncAt = Instant.now().toEpochMilli(),
        )
    }
}

package com.emm.data.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.HelloDb
import com.emm.domain.sync.SyncDebugState
import com.emm.domain.sync.SyncDebugStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class DefaultSyncDebugStateRepository(
    private val db: HelloDb,
    private val syncRuntimePolicy: SyncRuntimePolicy,
) : SyncDebugStateRepository {

    private val localFirstQueries = db.localFirstQueries

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(): Flow<SyncDebugState> {
        val accountStateFlow = localFirstQueries
            .selectLocalAccountState()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
        val deviceIdentityFlow = localFirstQueries
            .selectLocalDeviceIdentity()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
        val checkpointFlow = accountStateFlow.flatMapLatest { accountState ->
            val appAccountId = accountState?.appAccountId?.takeIf(String::isNotBlank)
                ?: return@flatMapLatest flowOf(null)
            localFirstQueries
                .selectSyncCheckpoint(appAccountId)
                .asFlow()
                .mapToOneOrNull(Dispatchers.IO)
        }
        val pendingFlow = accountStateFlow.flatMapLatest { accountState ->
            val appAccountId = accountState?.appAccountId?.takeIf(String::isNotBlank)
                ?: return@flatMapLatest flowOf(0L)
            localFirstQueries
                .countRetryableOperations(appAccountId, DrainOutbox.MAX_RETRY_COUNT)
                .asFlow()
                .mapToOne(Dispatchers.IO)
        }
        return combine(
            accountStateFlow,
            deviceIdentityFlow,
            checkpointFlow,
            pendingFlow,
        ) { accountState, deviceIdentity, checkpoint, pendingOps ->
            SyncDebugState(
                pendingOperations = pendingOps,
                lastSuccessfulSyncAt = checkpoint?.lastSuccessfulSyncAt,
                lastSyncError = checkpoint?.lastSyncError,
                deviceId = deviceIdentity?.deviceId,
                appAccountId = accountState?.appAccountId,
                modeLabel = syncRuntimePolicy.modeLabel,
                remoteAvailable = syncRuntimePolicy.remoteEnabled,
            )
        }
            .distinctUntilChanged()
    }
}

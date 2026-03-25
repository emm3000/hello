package com.emm.data.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.HelloDb
import com.emm.domain.sync.SyncDebugState
import com.emm.domain.sync.SyncDebugStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class DefaultSyncDebugStateRepository(
    private val db: HelloDb,
) : SyncDebugStateRepository {

    private val localFirstQueries = db.localFirstQueries

    override fun observe(): Flow<SyncDebugState> {
        val accountStateFlow = localFirstQueries.selectLocalAccountState().asFlow().mapToOneOrNull(Dispatchers.IO)
        val checkpointFlow = accountStateFlow
        val pendingFlow = accountStateFlow
        return combine(
            accountStateFlow,
            localFirstQueries.selectLocalDeviceIdentity().asFlow().mapToOneOrNull(Dispatchers.IO),
        ) { accountState, deviceIdentity ->
            val appAccountId = accountState?.appAccountId?.takeIf(String::isNotBlank)
            val pendingOps = appAccountId?.let {
                localFirstQueries.countRetryableOperations(it, DrainOutbox.MAX_RETRY_COUNT).executeAsOne()
            } ?: 0L
            val checkpoint = appAccountId?.let {
                localFirstQueries.selectSyncCheckpoint(it).executeAsOneOrNull()
            }
            SyncDebugState(
                pendingOperations = pendingOps,
                lastSuccessfulSyncAt = checkpoint?.lastSuccessfulSyncAt,
                lastSyncError = checkpoint?.lastSyncError,
                deviceId = deviceIdentity?.deviceId,
                appAccountId = accountState?.appAccountId,
            )
        }
    }
}

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

class DefaultSyncDebugStateRepository(
    db: HelloDb,
) : SyncDebugStateRepository {

    private val localFirstQueries = db.localFirstQueries

    override fun observe(): Flow<SyncDebugState> {
        return combine(
            localFirstQueries
                .countRetryableOperations(maxRetries = DrainOutbox.MAX_RETRY_COUNT)
                .asFlow()
                .mapToOne(Dispatchers.IO),
            localFirstQueries.selectSyncCheckpoint().asFlow().mapToOneOrNull(Dispatchers.IO),
            localFirstQueries.selectLocalDeviceIdentity().asFlow().mapToOneOrNull(Dispatchers.IO),
            localFirstQueries.selectLocalAccountState().asFlow().mapToOneOrNull(Dispatchers.IO),
        ) { pendingOps, checkpoint, deviceIdentity, accountState ->
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

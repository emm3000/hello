package com.emm.data.sync

import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.domain.sync.SyncEngine
import com.emm.domain.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

class DefaultSyncEngine(
    private val db: HelloDb,
    private val remote: SupabaseSyncRemoteDataSource,
    private val localDeviceIdentityProvider: LocalDeviceIdentityProvider,
    private val drainOutbox: DrainOutbox,
    private val pullRemoteOperations: PullRemoteOperations,
    private val applyRemoteOperation: ApplyRemoteOperation,
    private val ackOperations: AckOperations,
) : SyncEngine {

    private val localFirstQueries = db.localFirstQueries
    private val mutableState = MutableStateFlow(SyncState())

    override val state: StateFlow<SyncState> = mutableState.asStateFlow()

    override suspend fun runOnce() {
        val now = Instant.now().toEpochMilli()
        mutableState.value = mutableState.value.copy(
            isRunning = true,
            pendingOperations = pendingCount(),
            lastSyncError = null,
        )

        try {
            val localDeviceId = localDeviceIdentityProvider.getOrCreateDeviceId()
            remote.ensureAnonymousSession()
            val bootstrap = remote.bootstrapAnonymousDevice(deviceId = localDeviceId)
            persistLocalAccountState(
                appAccountId = bootstrap.appAccountId,
                authUserId = bootstrap.authUserId,
                updatedAt = now,
            )

            drainOutbox()
            pullApplyAndAck(localDeviceId = localDeviceId, now = now)

            mutableState.value = mutableState.value.copy(
                isRunning = false,
                pendingOperations = pendingCount(),
                lastSuccessfulSyncAt = now,
                lastSyncError = null,
            )
        } catch (e: Exception) {
            val checkpoint = localFirstQueries.selectSyncCheckpoint().executeAsOneOrNull()
            localFirstQueries.upsertSyncCheckpoint(
                lastPulledCursor = checkpoint?.lastPulledCursor ?: 0L,
                lastSuccessfulSyncAt = checkpoint?.lastSuccessfulSyncAt,
                lastSyncError = e.message ?: e::class.simpleName ?: "sync_failed",
                lastSyncErrorAt = now,
                updatedAt = now,
            )
            mutableState.value = mutableState.value.copy(
                isRunning = false,
                pendingOperations = pendingCount(),
                lastSyncError = e.message ?: "sync_failed",
            )
            throw e
        }
    }

    private suspend fun drainOutbox() {
        drainOutbox.invoke(batchSize = DrainOutbox.DEFAULT_PUSH_BATCH_SIZE)
    }

    private suspend fun pullApplyAndAck(localDeviceId: String, now: Long) {
        val pulledResult = pullRemoteOperations(limit = PullRemoteOperations.DEFAULT_PULL_BATCH_SIZE)

        if (pulledResult.operations.isEmpty()) {
            localFirstQueries.upsertSyncCheckpoint(
                lastPulledCursor = pulledResult.currentCursor,
                lastSuccessfulSyncAt = now,
                lastSyncError = null,
                lastSyncErrorAt = null,
                updatedAt = now,
            )
            return
        }

        var maxAckedCursor = pulledResult.currentCursor
        val ackedOpIds = mutableListOf<String>()

        db.transaction {
            pulledResult.operations.forEach { operation ->
                val existing = localFirstQueries.findProcessedRemoteOperation(operation.opId).executeAsOneOrNull()
                if (existing != null) {
                    ackedOpIds += operation.opId
                    if (operation.cursor > maxAckedCursor) {
                        maxAckedCursor = operation.cursor
                    }
                    return@forEach
                }

                val result = applyRemoteOperation(
                    operation = operation,
                    localDeviceId = localDeviceId,
                )

                if (result.shouldAck) {
                    localFirstQueries.markRemoteOperationProcessed(
                        opId = operation.opId,
                        cursor = operation.cursor,
                        entityType = operation.entityType,
                        entityId = operation.entityId,
                        operationType = operation.operationType,
                        processedAt = now,
                    )
                    ackedOpIds += operation.opId
                    if (operation.cursor > maxAckedCursor) {
                        maxAckedCursor = operation.cursor
                    }
                }
            }
        }

        ackOperations(ackedOpIds.distinct())

        localFirstQueries.upsertSyncCheckpoint(
            lastPulledCursor = maxAckedCursor,
            lastSuccessfulSyncAt = now,
            lastSyncError = null,
            lastSyncErrorAt = null,
            updatedAt = now,
        )
    }

    private fun persistLocalAccountState(appAccountId: String, authUserId: String, updatedAt: Long) {
        val current = localFirstQueries.selectLocalAccountState().executeAsOneOrNull()
        localFirstQueries.upsertLocalAccountState(
            appAccountId = appAccountId,
            authUserId = authUserId,
            pairingState = "Paired",
            createdAt = current?.createdAt ?: updatedAt,
            updatedAt = updatedAt,
        )
    }

    private fun pendingCount(): Long = localFirstQueries.countPendingOperations().executeAsOne()
}

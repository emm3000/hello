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
        var keepPulling = true
        while (keepPulling) {
            val pulledResult = pullRemoteOperations(limit = PullRemoteOperations.DEFAULT_PULL_BATCH_SIZE)

            if (pulledResult.operations.isEmpty()) {
                saveCheckpoint(cursor = pulledResult.currentCursor, now = now)
                keepPulling = false
            } else {
                val batchResult = applyBatch(pulledResult, localDeviceId, now)
                ackOperations(batchResult.ackedOpIds.distinct())
                saveCheckpoint(cursor = batchResult.maxCursor, now = now)
                // Stop pagination if a deferred op was encountered to avoid a re-fetch loop
                keepPulling = batchResult.firstDeferredCursor == null
            }
        }
    }

    private fun applyBatch(
        pulledResult: PullRemoteOperationsResult,
        localDeviceId: String,
        now: Long,
    ): BatchResult {
        var maxCursor = pulledResult.currentCursor
        var firstDeferredCursor: Long? = null
        val ackedOpIds = mutableListOf<String>()

        db.transaction {
            pulledResult.operations.forEach { operation ->
                val existing = localFirstQueries.findProcessedRemoteOperation(operation.opId).executeAsOneOrNull()
                if (existing != null) {
                    ackedOpIds += operation.opId
                    if (operation.cursor > maxCursor) maxCursor = operation.cursor
                    return@forEach
                }

                val result = applyRemoteOperation(operation = operation, localDeviceId = localDeviceId)

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
                    if (operation.cursor > maxCursor) maxCursor = operation.cursor
                } else {
                    if (firstDeferredCursor == null) firstDeferredCursor = operation.cursor
                }
            }
        }

        // Never advance the checkpoint past a deferred operation
        val cappedCursor = firstDeferredCursor?.let { deferredAt ->
            if (deferredAt - 1 < maxCursor) deferredAt - 1 else maxCursor
        } ?: maxCursor

        return BatchResult(
            ackedOpIds = ackedOpIds,
            firstDeferredCursor = firstDeferredCursor,
            maxCursor = cappedCursor,
        )
    }

    private fun saveCheckpoint(cursor: Long, now: Long) {
        localFirstQueries.upsertSyncCheckpoint(
            lastPulledCursor = cursor,
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

private data class BatchResult(
    val ackedOpIds: List<String>,
    val firstDeferredCursor: Long?,
    val maxCursor: Long,
)

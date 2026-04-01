package com.emm.data.sync

import com.emm.data.HelloDb
import com.emm.data.localfirst.currentAppAccountIdOrNull
import com.emm.data.localfirst.requireCurrentAppAccountId
import com.emm.data.logging.logError
import com.emm.data.logging.logInfo
import com.emm.domain.sync.SyncEngine
import com.emm.domain.sync.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Instant

class DefaultSyncEngine(
    private val db: HelloDb,
    private val identityBootstrapper: IdentityBootstrapper,
    private val drainOutbox: DrainOutbox,
    private val pullRemoteOperations: PullRemoteOperations,
    private val applyRemoteOperation: ApplyRemoteOperation,
    private val ackOperations: AckOperations,
) : SyncEngine {

    private val localFirstQueries = db.localFirstQueries
    private val mutableState = MutableStateFlow(SyncState())

    override val state: StateFlow<SyncState> = mutableState.asStateFlow()

    override suspend fun runOnce() {
        withContext(Dispatchers.IO) {
            val now = Instant.now().toEpochMilli()
            logInfo(TAG, "runOnce:start")
            mutableState.value = mutableState.value.copy(
                isRunning = true,
                pendingOperations = pendingCount(),
                lastSyncError = null,
            )

            try {
                identityBootstrapper.ensureIdentityReady()
                val localDeviceId = db.localFirstQueries
                    .selectLocalDeviceIdentity()
                    .executeAsOneOrNull()
                    ?.deviceId
                    .orEmpty()
                logInfo(TAG, "runOnce:identity_ready deviceId=$localDeviceId")

                logInfo(TAG, "runOnce:drain_outbox:start")
                drainOutbox()
                logInfo(TAG, "runOnce:drain_outbox:done pendingAfterDrain=${pendingCount()}")
                logInfo(TAG, "runOnce:pull_apply_ack:start")
                pullApplyAndAck(localDeviceId = localDeviceId, now = now)
                logInfo(TAG, "runOnce:pull_apply_ack:done")

                mutableState.value = mutableState.value.copy(
                    isRunning = false,
                    pendingOperations = pendingCount(),
                    lastSuccessfulSyncAt = now,
                    lastSyncError = null,
                )
                logInfo(TAG, "runOnce:success pending=${mutableState.value.pendingOperations}")
            } catch (e: Exception) {
                logError(TAG, "runOnce:error ${e.message}", e)
                db.currentAppAccountIdOrNull()?.let { appAccountId ->
                    val checkpoint = localFirstQueries.selectSyncCheckpoint(appAccountId).executeAsOneOrNull()
                    localFirstQueries.upsertSyncCheckpoint(
                        appAccountId = appAccountId,
                        lastPulledCursor = checkpoint?.lastPulledCursor ?: 0L,
                        lastSuccessfulSyncAt = checkpoint?.lastSuccessfulSyncAt,
                        lastSyncError = e.message ?: e::class.simpleName ?: "sync_failed",
                        lastSyncErrorAt = now,
                        updatedAt = now,
                    )
                }
                mutableState.value = mutableState.value.copy(
                    isRunning = false,
                    pendingOperations = pendingCount(),
                    lastSyncError = e.message ?: "sync_failed",
                )
                throw e
            }
        }
    }

    private suspend fun drainOutbox() {
        drainOutbox.invoke(batchSize = DrainOutbox.DEFAULT_PUSH_BATCH_SIZE)
    }

    private suspend fun pullApplyAndAck(localDeviceId: String, now: Long) {
        var keepPulling = true
        while (keepPulling) {
            val pulledResult = pullRemoteOperations(limit = PullRemoteOperations.DEFAULT_PULL_BATCH_SIZE)
            logInfo(
                TAG,
                "pullApplyAndAck:batch pulled=${pulledResult.operations.size} cursor=${pulledResult.currentCursor}"
            )

            if (pulledResult.operations.isEmpty()) {
                saveCheckpoint(cursor = pulledResult.currentCursor, now = now)
                keepPulling = false
            } else {
                val batchResult = applyBatch(pulledResult, localDeviceId, now)
                logInfo(
                    TAG,
                    "pullApplyAndAck:apply_result " +
                        "acked=${batchResult.ackedOpIds.size} " +
                        "deferredCursor=${batchResult.firstDeferredCursor} " +
                        "maxCursor=${batchResult.maxCursor}"
                )
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
        val appAccountId = db.requireCurrentAppAccountId()
        var maxCursor = pulledResult.currentCursor
        var firstDeferredCursor: Long? = null
        val ackedOpIds = mutableListOf<String>()

        db.transaction {
            pulledResult.operations.forEach { operation ->
                val existing = localFirstQueries
                    .findProcessedRemoteOperation(appAccountId, operation.opId)
                    .executeAsOneOrNull()
                if (existing != null) {
                    ackedOpIds += operation.opId
                    if (operation.cursor > maxCursor) maxCursor = operation.cursor
                    return@forEach
                }

                val result = applyRemoteOperation(operation = operation, localDeviceId = localDeviceId)

                if (result.shouldAck) {
                    localFirstQueries.markRemoteOperationProcessed(
                        appAccountId = appAccountId,
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

            // Advance the local Lamport clock to be >= the highest lamport seen in this batch.
            // This guarantees any subsequent local write has a lamport strictly greater than
            // all remote operations applied here, preserving distributed causality.
            val maxRemoteLamport = pulledResult.operations.maxOfOrNull { it.lamport }
            if (maxRemoteLamport != null) {
                localFirstQueries.advanceLamportTo(
                    value = maxRemoteLamport,
                    updatedAt = now,
                )
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
        val appAccountId = db.requireCurrentAppAccountId()
        localFirstQueries.upsertSyncCheckpoint(
            appAccountId = appAccountId,
            lastPulledCursor = cursor,
            lastSuccessfulSyncAt = now,
            lastSyncError = null,
            lastSyncErrorAt = null,
            updatedAt = now,
        )
    }

    private fun pendingCount(): Long {
        val appAccountId = db.currentAppAccountIdOrNull() ?: return 0L
        return localFirstQueries
            .countRetryableOperations(appAccountId, maxRetries = DrainOutbox.MAX_RETRY_COUNT)
            .executeAsOne()
    }
}

private const val TAG = "SyncEngine"

private data class BatchResult(
    val ackedOpIds: List<String>,
    val firstDeferredCursor: Long?,
    val maxCursor: Long,
)

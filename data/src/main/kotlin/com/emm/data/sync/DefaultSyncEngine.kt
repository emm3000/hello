package com.emm.data.sync

import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.domain.sync.SyncEngine
import com.emm.domain.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

class DefaultSyncEngine(
    private val db: HelloDb,
    private val remote: SupabaseSyncRemoteDataSource,
    private val localDeviceIdentityProvider: LocalDeviceIdentityProvider,
) : SyncEngine {

    private val localFirstQueries = db.localFirstQueries
    private val deckQueries = db.deckQueries
    private val mutableState = MutableStateFlow(SyncState())

    override val state: StateFlow<SyncState> = mutableState.asStateFlow()

    override suspend fun runOnce() {
        val now = Instant.now().toEpochMilli()
        val currentPending = pendingCount()
        mutableState.value = mutableState.value.copy(
            isRunning = true,
            pendingOperations = currentPending,
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

            pushPendingOperations(now = now)
            pullAndApplyRemoteOperations(now = now, localDeviceId = localDeviceId)

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

    private suspend fun pushPendingOperations(now: Long) {
        val pending = localFirstQueries.pendingOperations(PUSH_BATCH_SIZE).executeAsList()
        if (pending.isEmpty()) return

        val response = remote.push(pending)
        val accepted = response.acceptedOpIds.toSet()
        val rejected = response.rejected.associateBy({ it.opId }, { it.reason })

        pending.forEach { operation ->
            when {
                accepted.contains(operation.opId) -> {
                    localFirstQueries.markOperationAcked(
                        lastAttemptAt = now,
                        opId = operation.opId,
                    )
                }
                rejected.containsKey(operation.opId) -> {
                    localFirstQueries.markOperationDead(
                        lastAttemptAt = now,
                        lastError = rejected.getValue(operation.opId),
                        opId = operation.opId,
                    )
                }
            }
        }
    }

    private suspend fun pullAndApplyRemoteOperations(now: Long, localDeviceId: String) {
        val checkpoint = localFirstQueries.selectSyncCheckpoint().executeAsOneOrNull()
        val currentCursor = checkpoint?.lastPulledCursor ?: 0L
        val pulled = remote.pull(cursor = currentCursor, limit = PULL_BATCH_SIZE)
        if (pulled.isEmpty()) {
            localFirstQueries.upsertSyncCheckpoint(
                lastPulledCursor = currentCursor,
                lastSuccessfulSyncAt = now,
                lastSyncError = null,
                lastSyncErrorAt = null,
                updatedAt = now,
            )
            return
        }

        var maxCursor = currentCursor
        val ackedOpIds = mutableListOf<String>()
        db.transaction {
            pulled.forEach { operation ->
                if (operation.cursor > maxCursor) {
                    maxCursor = operation.cursor
                }
                val applied = applyRemoteOperation(
                    operation = operation,
                    localDeviceId = localDeviceId,
                )
                if (applied) {
                    ackedOpIds += operation.opId
                }
            }
        }

        remote.ack(ackedOpIds)
        localFirstQueries.upsertSyncCheckpoint(
            lastPulledCursor = maxCursor,
            lastSuccessfulSyncAt = now,
            lastSyncError = null,
            lastSyncErrorAt = null,
            updatedAt = now,
        )
    }

    private fun applyRemoteOperation(operation: RemoteSyncOperation, localDeviceId: String): Boolean {
        if (!operation.entityType.equals("deck", ignoreCase = true)) {
            return false
        }
        return applyDeckOperation(operation, localDeviceId)
    }

    private fun applyDeckOperation(operation: RemoteSyncOperation, localDeviceId: String): Boolean {
        val existing = deckQueries.findById(operation.entityId).executeAsOneOrNull()
        if (existing != null && operation.lamport < existing.versionLamport) {
            return false
        }

        val operationType = operation.operationType.lowercase()
        val payload = operation.payload
        val payloadName = payload.stringOrNull("name")
        val payloadDescription = payload.stringOrNull("description")
        val payloadCreatedAt = payload.epochFromIso("created_at")
        val payloadUpdatedAt = payload.epochFromIso("updated_at")
        val payloadDeletedAt = payload.epochFromIso("deleted_at")
        val operationCreatedAt = parseIsoToEpoch(operation.createdAt) ?: Instant.now().toEpochMilli()

        if (operationType != "delete" && payloadName.isNullOrBlank()) {
            return false
        }

        val createdAt = existing?.createdAt ?: payloadCreatedAt ?: operationCreatedAt
        val updatedAt = payloadUpdatedAt ?: operationCreatedAt
        val deletedAt = when (operationType) {
            "delete" -> payloadDeletedAt ?: operationCreatedAt
            else -> payloadDeletedAt
        }
        val name = when (operationType) {
            "delete" -> existing?.name ?: payloadName ?: "[deleted]"
            else -> payloadName.orEmpty()
        }
        val description = payloadDescription ?: existing?.description

        deckQueries.insert(
            id = operation.entityId,
            name = name,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            originDeviceId = existing?.originDeviceId ?: operation.originDeviceId.ifBlank { localDeviceId },
            lastModifiedByDeviceId = operation.originDeviceId.ifBlank { localDeviceId },
            versionLamport = operation.lamport,
        )
        return true
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

    private fun JsonObject.stringOrNull(key: String): String? {
        return this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.epochFromIso(key: String): Long? {
        return parseIsoToEpoch(stringOrNull(key))
    }

    private fun parseIsoToEpoch(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
    }

    companion object {
        private const val PUSH_BATCH_SIZE = 100L
        private const val PULL_BATCH_SIZE = 200
    }
}

package com.emm.data.sync

import com.emm.data.HelloDb
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

class ApplyRemoteOperation(
    private val db: HelloDb,
) {

    private val deckQueries = db.deckQueries

    operator fun invoke(
        operation: RemoteSyncOperation,
        localDeviceId: String,
    ): ApplyRemoteOperationResult {
        if (!operation.entityType.equals("deck", ignoreCase = true)) {
            return ApplyRemoteOperationResult.Ignored
        }

        val existing = deckQueries.findById(operation.entityId).executeAsOneOrNull()
        if (existing != null && operation.lamport < existing.versionLamport) {
            return ApplyRemoteOperationResult.Ignored
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
            return ApplyRemoteOperationResult.Ignored
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

        return ApplyRemoteOperationResult.Applied(
            opId = operation.opId,
            cursor = operation.cursor,
            entityId = operation.entityId,
            operationType = operationType,
        )
    }

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
}

sealed interface ApplyRemoteOperationResult {
    data object Ignored : ApplyRemoteOperationResult

    data class Applied(
        val opId: String,
        val cursor: Long,
        val entityId: String,
        val operationType: String,
    ) : ApplyRemoteOperationResult
}

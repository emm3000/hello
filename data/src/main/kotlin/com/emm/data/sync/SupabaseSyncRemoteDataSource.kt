package com.emm.data.sync

import com.emm.data.OperationLog
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class SupabaseSyncRemoteDataSource(
    private val supabase: SupabaseClient,
    private val json: Json,
) {

    suspend fun ensureAnonymousSession() {
        supabase.auth.awaitInitialization()
        if (supabase.auth.currentSessionOrNull() == null) {
            supabase.auth.signInAnonymously()
        }
    }

    suspend fun bootstrapAnonymousDevice(
        deviceId: String,
        deviceName: String? = null,
        platform: String = "android",
    ): SyncBootstrapResponse {
        val result = supabase.postgrest.rpc(
            function = "sync_bootstrap_anonymous",
            parameters = buildJsonObject {
                put("p_device_id", deviceId)
                put("p_device_name", deviceName?.takeIf { it.isNotBlank() })
                put("p_platform", platform)
            }
        )
        return result.decodeAs()
    }

    suspend fun createPairingSession(ttlMinutes: Int = 10): PairingSessionResponse {
        val result = supabase.postgrest.rpc(
            function = "create_pairing_session",
            parameters = buildJsonObject {
                put("p_ttl_minutes", ttlMinutes)
            }
        )
        return result.decodeAs()
    }

    suspend fun redeemPairingCode(
        code: String,
        deviceId: String? = null,
        deviceName: String? = null,
        platform: String = "android",
    ): PairingRedeemResponse {
        val result = supabase.postgrest.rpc(
            function = "redeem_pairing_code",
            parameters = buildJsonObject {
                put("p_code", code)
                put("p_device_id", deviceId?.takeIf { it.isNotBlank() })
                put("p_device_name", deviceName?.takeIf { it.isNotBlank() })
                put("p_platform", platform)
            }
        )
        return result.decodeAs()
    }

    suspend fun listLinkedDevices(): List<LinkedDevice> {
        val result = supabase.postgrest.rpc(function = "list_linked_devices")
        return result.decodeList()
    }

    suspend fun revokeLinkedDevice(deviceId: String, reason: String? = null): Boolean {
        val result = supabase.postgrest.rpc(
            function = "revoke_linked_device",
            parameters = buildJsonObject {
                put("p_device_id", deviceId)
                put("p_reason", reason?.takeIf { it.isNotBlank() })
            }
        )
        return result.decodeAs()
    }

    suspend fun push(operations: List<OperationLog>): SyncPushResponse {
        val batch = buildJsonArray {
            operations.forEach { operation ->
                add(
                    buildJsonObject {
                        put("op_id", operation.opId)
                        put("entity_type", operation.entityType.lowercase())
                        put("entity_id", operation.entityId)
                        put("operation_type", mapOperationType(operation.operationType))
                        put("lamport", operation.lamport)
                        put("payload", operation.payload.safeJsonPayload())
                    }
                )
            }
        }
        val result = supabase.postgrest.rpc(
            function = "sync_push",
            parameters = buildJsonObject {
                put("batch", batch)
            }
        )
        return result.decodeAs()
    }

    suspend fun pull(cursor: Long, limit: Int): List<RemoteSyncOperation> {
        val result = supabase.postgrest.rpc(
            function = "sync_pull",
            parameters = buildJsonObject {
                put("p_cursor", cursor)
                put("p_limit", limit)
            }
        )
        return result.decodeList()
    }

    suspend fun ack(opIds: List<String>): Int {
        if (opIds.isEmpty()) return 0
        val result = supabase.postgrest.rpc(
            function = "sync_ack",
            parameters = buildJsonObject {
                putJsonArray("op_ids") {
                    opIds.forEach { add(JsonPrimitive(it)) }
                }
            }
        )
        return result.decodeAs()
    }

    private fun mapOperationType(raw: String): String {
        return when (raw.lowercase()) {
            "create" -> "create"
            "update" -> "update"
            "delete" -> "delete"
            else -> "upsert"
        }
    }

    private fun String.safeJsonPayload(): JsonObject {
        return runCatching {
            json.parseToJsonElement(this).jsonObject
        }.getOrElse {
            buildJsonObject {
                put("raw_payload", this@safeJsonPayload)
            }
        }
    }
}

@Serializable
data class SyncBootstrapResponse(
    @SerialName("app_account_id")
    val appAccountId: String,
    @SerialName("app_device_id")
    val appDeviceId: String,
    @SerialName("auth_user_id")
    val authUserId: String,
    val created: Boolean,
)

@Serializable
data class PairingSessionResponse(
    @SerialName("pairing_session_id")
    val pairingSessionId: String,
    val code: String,
    @SerialName("expires_at")
    val expiresAt: String,
)

@Serializable
data class PairingRedeemResponse(
    @SerialName("app_account_id")
    val appAccountId: String,
    @SerialName("app_device_id")
    val appDeviceId: String,
    @SerialName("auth_user_id")
    val authUserId: String,
    @SerialName("pairing_session_id")
    val pairingSessionId: String,
    val joined: Boolean,
)

@Serializable
data class LinkedDevice(
    val id: String,
    @SerialName("device_name")
    val deviceName: String? = null,
    val platform: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("last_seen_at")
    val lastSeenAt: String? = null,
    @SerialName("revoked_at")
    val revokedAt: String? = null,
    @SerialName("is_current")
    val isCurrent: Boolean,
)

@Serializable
data class SyncPushResponse(
    @SerialName("accepted_op_ids")
    val acceptedOpIds: List<String> = emptyList(),
    val rejected: List<SyncPushRejectedOperation> = emptyList(),
)

@Serializable
data class SyncPushRejectedOperation(
    @SerialName("op_id")
    val opId: String,
    val reason: String,
)

@Serializable
data class RemoteSyncOperation(
    val cursor: Long,
    @SerialName("op_id")
    val opId: String,
    @SerialName("entity_type")
    val entityType: String,
    @SerialName("entity_id")
    val entityId: String,
    @SerialName("operation_type")
    val operationType: String,
    val payload: JsonObject,
    val lamport: Long,
    @SerialName("origin_device_id")
    val originDeviceId: String,
    @SerialName("created_at")
    val createdAt: String,
)

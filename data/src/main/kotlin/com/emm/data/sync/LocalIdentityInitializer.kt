package com.emm.data.sync

import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import java.time.Instant

interface LocalIdentityInitializer {
    suspend fun ensureReady(): LocalIdentityState
}

data class LocalIdentityState(
    val deviceId: String,
    val appAccountId: String,
    val pairingState: String,
    val createdLocalAccount: Boolean,
)

class DefaultLocalIdentityInitializer(
    private val db: HelloDb,
    private val localDeviceIdentityProvider: LocalDeviceIdentityProvider,
) : LocalIdentityInitializer {

    override suspend fun ensureReady(): LocalIdentityState {
        val deviceId = localDeviceIdentityProvider.getOrCreateDeviceId()
        val current = db.localFirstQueries.selectLocalAccountState().executeAsOneOrNull()
        val currentAppAccountId = current?.appAccountId?.takeIf(String::isNotBlank)
        if (currentAppAccountId != null) {
            return LocalIdentityState(
                deviceId = deviceId,
                appAccountId = currentAppAccountId,
                pairingState = current.pairingState,
                createdLocalAccount = false,
            )
        }

        val now = Instant.now().toEpochMilli()
        val syntheticAccountId = "local-only:$deviceId"
        db.localFirstQueries.upsertLocalAccountState(
            appAccountId = syntheticAccountId,
            authUserId = current?.authUserId,
            pairingState = LOCAL_ONLY_PAIRING_STATE,
            createdAt = current?.createdAt ?: now,
            updatedAt = now,
        )

        return LocalIdentityState(
            deviceId = deviceId,
            appAccountId = syntheticAccountId,
            pairingState = LOCAL_ONLY_PAIRING_STATE,
            createdLocalAccount = true,
        )
    }
}

const val LOCAL_ONLY_PAIRING_STATE = "LocalOnly"

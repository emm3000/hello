package com.emm.data.localfirst

import com.emm.data.HelloDb

interface LocalIdentityInitializer {
    suspend fun ensureReady(): LocalIdentityState
}

data class LocalIdentityState(
    val deviceId: String,
    val createdInstallation: Boolean,
)

class DefaultLocalIdentityInitializer(
    private val db: HelloDb,
    private val localDeviceIdentityProvider: LocalDeviceIdentityProvider,
) : LocalIdentityInitializer {

    override suspend fun ensureReady(): LocalIdentityState {
        val existingIdentity = db.localFirstQueries.selectLocalDeviceIdentity().executeAsOneOrNull()
        val deviceId = localDeviceIdentityProvider.getOrCreateDeviceId()
        return LocalIdentityState(
            deviceId = deviceId,
            createdInstallation = existingIdentity == null,
        )
    }
}

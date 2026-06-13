package com.emm.data.localfirst

import com.emm.data.HelloDb
import com.emm.domain.localfirst.LocalIdentityInitializer
import com.emm.domain.localfirst.LocalIdentityState

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

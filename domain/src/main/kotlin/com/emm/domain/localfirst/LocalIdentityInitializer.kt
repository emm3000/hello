package com.emm.domain.localfirst

interface LocalIdentityInitializer {
    suspend fun ensureReady(): LocalIdentityState
}

data class LocalIdentityState(
    val deviceId: String,
    val createdInstallation: Boolean,
)

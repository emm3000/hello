package com.emm.domain.sync

class RevokeLinkedDeviceUseCase(
    private val repository: PairingRepository,
) {
    suspend fun execute(deviceId: String, reason: String? = null): Boolean {
        return repository.revokeLinkedDevice(deviceId, reason)
    }
}

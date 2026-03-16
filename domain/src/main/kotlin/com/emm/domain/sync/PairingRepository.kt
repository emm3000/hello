package com.emm.domain.sync

interface PairingRepository {
    suspend fun ensureLinkedIdentity()
    suspend fun createPairingSession(ttlMinutes: Int = 10): PairingSession
    suspend fun redeemPairingCode(code: String)
    suspend fun listLinkedDevices(): List<LinkedDevice>
    suspend fun revokeLinkedDevice(deviceId: String, reason: String? = null): Boolean
}

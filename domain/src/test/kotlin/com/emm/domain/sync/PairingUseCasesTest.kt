package com.emm.domain.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingUseCasesTest {

    @Test
    fun ensureLinkedIdentity_callsRepository() = runBlocking {
        val repository = FakePairingRepository()
        val useCase = EnsureLinkedIdentityUseCase(repository)

        useCase()

        assertTrue(repository.ensureLinkedIdentityCalled)
    }

    @Test
    fun createPairingSession_forwardsTtlAndReturnsSession() = runBlocking {
        val repository = FakePairingRepository()
        val useCase = CreatePairingSessionUseCase(repository)

        val session = useCase(ttlMinutes = 15)

        assertEquals(15, repository.lastCreateTtlMinutes)
        assertEquals(repository.pairingSessionToReturn, session)
    }

    @Test
    fun redeemPairingCode_forwardsCode() = runBlocking {
        val repository = FakePairingRepository()
        val useCase = RedeemPairingCodeUseCase(repository)

        useCase("ABCD12")

        assertEquals("ABCD12", repository.lastRedeemedCode)
    }

    @Test
    fun listLinkedDevices_returnsRepositoryList() = runBlocking {
        val repository = FakePairingRepository()
        val useCase = ListLinkedDevicesUseCase(repository)

        val devices = useCase()

        assertEquals(repository.devicesToReturn, devices)
    }

    @Test
    fun revokeLinkedDevice_forwardsArgumentsAndReturnsResult() = runBlocking {
        val repository = FakePairingRepository().apply { revokeResult = true }
        val useCase = RevokeLinkedDeviceUseCase(repository)

        val result = useCase(deviceId = "device-2", reason = "lost")

        assertEquals("device-2", repository.lastRevokedDeviceId)
        assertEquals("lost", repository.lastRevokeReason)
        assertTrue(result)
    }

    private class FakePairingRepository : PairingRepository {
        var ensureLinkedIdentityCalled: Boolean = false
        var lastCreateTtlMinutes: Int? = null
        var lastRedeemedCode: String? = null
        var lastRevokedDeviceId: String? = null
        var lastRevokeReason: String? = null
        var revokeResult: Boolean = false

        val pairingSessionToReturn = PairingSession(
            code = "PAIR01",
            expiresAt = "2026-03-16T12:00:00Z",
        )

        val devicesToReturn = listOf(
            LinkedDevice(
                id = "device-1",
                deviceName = "Pixel",
                platform = "android",
                createdAt = "2026-03-10T10:00:00Z",
                lastSeenAt = null,
                revokedAt = null,
                isCurrent = true,
            )
        )

        override suspend fun ensureLinkedIdentity() {
            ensureLinkedIdentityCalled = true
        }

        override suspend fun createPairingSession(ttlMinutes: Int): PairingSession {
            lastCreateTtlMinutes = ttlMinutes
            return pairingSessionToReturn
        }

        override suspend fun redeemPairingCode(code: String) {
            lastRedeemedCode = code
        }

        override suspend fun listLinkedDevices(): List<LinkedDevice> {
            return devicesToReturn
        }

        override suspend fun revokeLinkedDevice(deviceId: String, reason: String?): Boolean {
            lastRevokedDeviceId = deviceId
            lastRevokeReason = reason
            return revokeResult
        }
    }
}

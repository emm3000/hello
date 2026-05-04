package com.emm.hello.startup

import com.emm.data.sync.LocalIdentityInitializer
import com.emm.data.sync.LocalIdentityState
import com.emm.data.sync.SyncRuntimePolicy
import com.emm.domain.sync.EnsureLinkedIdentityUseCase
import com.emm.domain.sync.PairingRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppStartupCoordinatorTest {

    @Test
    fun `local-only startup prepares local identity and disables remote runtime`() = runTest {
        val pairingRepository = FakePairingRepository()
        val localIdentityInitializer = FakeLocalIdentityInitializer()
        val syncRuntimeController = FakeSyncRuntimeController()
        val subject = AppStartupCoordinator(
            ensureLinkedIdentityUseCase = EnsureLinkedIdentityUseCase(pairingRepository),
            localIdentityInitializer = localIdentityInitializer,
            syncRuntimePolicy = FakeSyncRuntimePolicy(remoteEnabled = false),
            syncRuntimeController = syncRuntimeController,
            scope = this,
        )

        subject.start()
        runCurrent()
        advanceUntilIdle()

        assertThat(localIdentityInitializer.calls).isEqualTo(1)
        assertThat(pairingRepository.ensureCalls).isEqualTo(0)
        assertThat(syncRuntimeController.startCalls).isEqualTo(0)
        assertThat(syncRuntimeController.stopCalls).isEqualTo(1)
        assertThat(subject.state.value).isEqualTo(
            AppStartupState.Ready(isLocalOnly = true, modeLabel = "local-only")
        )
    }

    @Test
    fun `remote startup prepares identity enables runtime and links remote account`() = runTest {
        val pairingRepository = FakePairingRepository()
        val localIdentityInitializer = FakeLocalIdentityInitializer()
        val syncRuntimeController = FakeSyncRuntimeController()
        val subject = AppStartupCoordinator(
            ensureLinkedIdentityUseCase = EnsureLinkedIdentityUseCase(pairingRepository),
            localIdentityInitializer = localIdentityInitializer,
            syncRuntimePolicy = FakeSyncRuntimePolicy(remoteEnabled = true),
            syncRuntimeController = syncRuntimeController,
            scope = this,
        )

        subject.start()
        runCurrent()
        advanceUntilIdle()

        assertThat(localIdentityInitializer.calls).isEqualTo(1)
        assertThat(pairingRepository.ensureCalls).isEqualTo(1)
        assertThat(syncRuntimeController.startCalls).isEqualTo(1)
        assertThat(syncRuntimeController.stopCalls).isEqualTo(0)
        assertThat(subject.state.value).isEqualTo(
            AppStartupState.Ready(isLocalOnly = false, modeLabel = "remote")
        )
    }

    private class FakeSyncRuntimePolicy(
        override val remoteEnabled: Boolean,
    ) : SyncRuntimePolicy {
        override val modeLabel: String = if (remoteEnabled) "remote" else "local-only"
    }

    private class FakeSyncRuntimeController : SyncRuntimeController {
        var startCalls: Int = 0
        var stopCalls: Int = 0

        override fun start() {
            startCalls += 1
        }

        override fun stop() {
            stopCalls += 1
        }
    }

    private class FakeLocalIdentityInitializer : LocalIdentityInitializer {
        var calls: Int = 0

        override suspend fun ensureReady(): LocalIdentityState {
            calls += 1
            return LocalIdentityState(
                deviceId = "device-1",
                appAccountId = "local-only:device-1",
                pairingState = "LocalOnly",
                createdLocalAccount = true,
            )
        }
    }

    private class FakePairingRepository : PairingRepository {
        var ensureCalls: Int = 0

        override suspend fun ensureLinkedIdentity() {
            ensureCalls += 1
        }

        override suspend fun createPairingSession(ttlMinutes: Int) = error("unused")

        override suspend fun redeemPairingCode(code: String) = error("unused")

        override suspend fun listLinkedDevices() = error("unused")

        override suspend fun revokeLinkedDevice(deviceId: String, reason: String?) = error("unused")
    }
}

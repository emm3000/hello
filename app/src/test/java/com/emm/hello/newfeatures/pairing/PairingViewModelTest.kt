package com.emm.hello.newfeatures.pairing

import app.cash.turbine.test
import com.emm.data.sync.SyncRuntimePolicy
import com.emm.domain.sync.CreatePairingSessionUseCase
import com.emm.domain.sync.EnsureLinkedIdentityUseCase
import com.emm.domain.sync.LinkedDevice
import com.emm.domain.sync.ListLinkedDevicesUseCase
import com.emm.domain.sync.PairingRepository
import com.emm.domain.sync.PairingSession
import com.emm.domain.sync.RedeemPairingCodeUseCase
import com.emm.domain.sync.RevokeLinkedDeviceUseCase
import com.emm.domain.sync.SyncEngine
import com.emm.domain.sync.SyncState
import com.emm.hello.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `join code changed filters non-digits and truncates to 6 chars`() = runTest {
        val viewModel = makeViewModel()
        advanceUntilIdle()

        // take(6) runs before filter: "1234567".take(6) = "123456", then filter = "123456"
        viewModel.onIntent(PairingUiIntent.JoinCodeChanged("1234567"))
        assertThat(viewModel.uiState.value.joinCode).isEqualTo("123456")

        // "12a3b4".take(6) = "12a3b4", filter digits = "1234"
        viewModel.onIntent(PairingUiIntent.JoinCodeChanged("12a3b4"))
        assertThat(viewModel.uiState.value.joinCode).isEqualTo("1234")
    }

    @Test
    fun `join with code shorter than 6 digits emits invalid code message`() = runTest {
        val viewModel = makeViewModel()
        advanceUntilIdle()

        viewModel.onIntent(PairingUiIntent.JoinCodeChanged("123"))

        viewModel.effect.test {
            viewModel.onIntent(PairingUiIntent.JoinWithCodeClicked)
            val effect = awaitItem()
            assertThat(effect).isEqualTo(PairingUiEffect.ShowMessage("Ingresa un código válido de 6 dígitos"))
        }
    }

    @Test
    fun `init triggers refresh devices and populates devices list`() = runTest {
        val device = LinkedDevice(id = "d1", createdAt = "2026-01-01", isCurrent = true)
        val viewModel = makeViewModel(pairingRepo = FakePairingRepo(devices = listOf(device)))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.devices).containsExactly(device)
    }

    @Test
    fun `local only mode skips initial remote refresh and marks remote unavailable`() = runTest {
        val pairingRepo = FakePairingRepo(devices = listOf(LinkedDevice(id = "d1", createdAt = "2026-01-01", isCurrent = false)))
        val viewModel = makeViewModel(
            pairingRepo = pairingRepo,
            syncRuntimePolicy = FakeSyncRuntimePolicy(remoteEnabled = false, modeLabel = "local-only"),
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.remoteAvailable).isFalse()
        assertThat(viewModel.uiState.value.modeLabel).isEqualTo("local-only")
        assertThat(pairingRepo.listDevicesCalls).isEqualTo(0)
    }

    @Test
    fun `create code success sets generated code in state and emits message`() = runTest {
        val session = PairingSession("654321", "2026-03-18T11:00:00")
        val viewModel = makeViewModel(pairingRepo = FakePairingRepo(session = session))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(PairingUiIntent.CreateCodeClicked)
            assertThat(awaitItem()).isEqualTo(PairingUiEffect.ShowMessage("Código generado"))
        }

        assertThat(viewModel.uiState.value.generatedCode).isEqualTo("654321")
    }

    @Test
    fun `create code failure clears loading and emits error message`() = runTest {
        val viewModel = makeViewModel(pairingRepo = FakePairingRepo(shouldFail = true))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(PairingUiIntent.CreateCodeClicked)
            val effect = awaitItem()
            assertThat(effect).isInstanceOf(PairingUiEffect.ShowMessage::class.java)
            assertThat((effect as PairingUiEffect.ShowMessage).message).isEqualTo("create error")
        }

        assertThat(viewModel.uiState.value.isGeneratingCode).isFalse()
    }

    @Test
    fun `join with code success clears join code in state and emits linked message`() = runTest {
        val viewModel = makeViewModel()
        advanceUntilIdle()

        viewModel.onIntent(PairingUiIntent.JoinCodeChanged("123456"))

        viewModel.effect.test {
            viewModel.onIntent(PairingUiIntent.JoinWithCodeClicked)
            assertThat(awaitItem()).isEqualTo(PairingUiEffect.ShowMessage("Dispositivo vinculado"))
        }

        assertThat(viewModel.uiState.value.joinCode).isEmpty()
    }

    @Test
    fun `revoke device success emits revoked message`() = runTest {
        val viewModel = makeViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(PairingUiIntent.RevokeDeviceClicked("device-1"))
            assertThat(awaitItem()).isEqualTo(PairingUiEffect.ShowMessage("Dispositivo revocado"))
        }
    }

    @Test
    fun `create code in local only emits unavailable message without remote calls`() = runTest {
        val pairingRepo = FakePairingRepo()
        val syncEngine = FakeSyncEngine()
        val viewModel = makeViewModel(
            pairingRepo = pairingRepo,
            syncEngine = syncEngine,
            syncRuntimePolicy = FakeSyncRuntimePolicy(remoteEnabled = false, modeLabel = "local-only"),
        )
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(PairingUiIntent.CreateCodeClicked)
            assertThat(awaitItem()).isEqualTo(
                PairingUiEffect.ShowMessage(
                    "La vinculación remota está temporalmente fuera de servicio en modo local-only"
                )
            )
        }

        assertThat(pairingRepo.ensureLinkedIdentityCalls).isEqualTo(0)
        assertThat(pairingRepo.createPairingSessionCalls).isEqualTo(0)
        assertThat(syncEngine.runOnceCalls).isEqualTo(0)
    }

    @Test
    fun `refresh devices in local only emits unavailable message without remote calls`() = runTest {
        val pairingRepo = FakePairingRepo()
        val viewModel = makeViewModel(
            pairingRepo = pairingRepo,
            syncRuntimePolicy = FakeSyncRuntimePolicy(remoteEnabled = false, modeLabel = "local-only"),
        )
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(PairingUiIntent.RefreshDevicesClicked)
            assertThat(awaitItem()).isEqualTo(
                PairingUiEffect.ShowMessage(
                    "La vinculación remota está temporalmente fuera de servicio en modo local-only"
                )
            )
        }

        assertThat(pairingRepo.listDevicesCalls).isEqualTo(0)
    }

    @Test
    fun `join with code in local only emits unavailable message without remote calls`() = runTest {
        val pairingRepo = FakePairingRepo()
        val syncEngine = FakeSyncEngine()
        val viewModel = makeViewModel(
            pairingRepo = pairingRepo,
            syncEngine = syncEngine,
            syncRuntimePolicy = FakeSyncRuntimePolicy(remoteEnabled = false, modeLabel = "local-only"),
        )
        advanceUntilIdle()

        viewModel.onIntent(PairingUiIntent.JoinCodeChanged("123456"))

        viewModel.effect.test {
            viewModel.onIntent(PairingUiIntent.JoinWithCodeClicked)
            assertThat(awaitItem()).isEqualTo(
                PairingUiEffect.ShowMessage(
                    "La vinculación remota está temporalmente fuera de servicio en modo local-only"
                )
            )
        }

        assertThat(pairingRepo.redeemPairingCodeCalls).isEqualTo(0)
        assertThat(pairingRepo.ensureLinkedIdentityCalls).isEqualTo(0)
        assertThat(syncEngine.runOnceCalls).isEqualTo(0)
    }

    @Test
    fun `revoke device in local only emits unavailable message without remote calls`() = runTest {
        val pairingRepo = FakePairingRepo()
        val viewModel = makeViewModel(
            pairingRepo = pairingRepo,
            syncRuntimePolicy = FakeSyncRuntimePolicy(remoteEnabled = false, modeLabel = "local-only"),
        )
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(PairingUiIntent.RevokeDeviceClicked("device-1"))
            assertThat(awaitItem()).isEqualTo(
                PairingUiEffect.ShowMessage(
                    "La vinculación remota está temporalmente fuera de servicio en modo local-only"
                )
            )
        }

        assertThat(pairingRepo.ensureLinkedIdentityCalls).isEqualTo(0)
        assertThat(pairingRepo.revokeLinkedDeviceCalls).isEqualTo(0)
    }

    private fun makeViewModel(
        pairingRepo: FakePairingRepo = FakePairingRepo(),
        syncEngine: FakeSyncEngine = FakeSyncEngine(),
        syncRuntimePolicy: SyncRuntimePolicy = FakeSyncRuntimePolicy(),
    ): PairingViewModel = PairingViewModel(
        ensureLinkedIdentityUseCase = EnsureLinkedIdentityUseCase(pairingRepo),
        createPairingSessionUseCase = CreatePairingSessionUseCase(pairingRepo),
        redeemPairingCodeUseCase = RedeemPairingCodeUseCase(pairingRepo),
        listLinkedDevicesUseCase = ListLinkedDevicesUseCase(pairingRepo),
        revokeLinkedDeviceUseCase = RevokeLinkedDeviceUseCase(pairingRepo),
        syncEngine = syncEngine,
        syncRuntimePolicy = syncRuntimePolicy,
    )

    private class FakePairingRepo(
        private val session: PairingSession = PairingSession("123456", "2026-03-18T10:00:00"),
        private val devices: List<LinkedDevice> = emptyList(),
        private val shouldFail: Boolean = false,
    ) : PairingRepository {
        var ensureLinkedIdentityCalls: Int = 0
        var createPairingSessionCalls: Int = 0
        var redeemPairingCodeCalls: Int = 0
        var listDevicesCalls: Int = 0
        var revokeLinkedDeviceCalls: Int = 0
        override suspend fun ensureLinkedIdentity() {
            ensureLinkedIdentityCalls += 1
        }
        override suspend fun createPairingSession(ttlMinutes: Int): PairingSession {
            createPairingSessionCalls += 1
            if (shouldFail) error("create error")
            return session
        }
        override suspend fun redeemPairingCode(code: String) {
            redeemPairingCodeCalls += 1
            if (shouldFail) error("redeem error")
        }
        override suspend fun listLinkedDevices(): List<LinkedDevice> {
            listDevicesCalls += 1
            return devices
        }
        override suspend fun revokeLinkedDevice(deviceId: String, reason: String?): Boolean {
            revokeLinkedDeviceCalls += 1
            if (shouldFail) error("revoke error")
            return true
        }
    }

    private class FakeSyncEngine(private val shouldFail: Boolean = false) : SyncEngine {
        var runOnceCalls: Int = 0
        override val state = MutableStateFlow(SyncState())
        override suspend fun runOnce() {
            runOnceCalls += 1
            if (shouldFail) error("sync error")
        }
    }

    private class FakeSyncRuntimePolicy(
        override val remoteEnabled: Boolean = true,
        override val modeLabel: String = "remote",
    ) : SyncRuntimePolicy
}

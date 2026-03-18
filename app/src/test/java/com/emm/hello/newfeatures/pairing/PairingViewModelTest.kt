package com.emm.hello.newfeatures.pairing

import app.cash.turbine.test
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

    private fun makeViewModel(
        pairingRepo: FakePairingRepo = FakePairingRepo(),
        syncEngine: FakeSyncEngine = FakeSyncEngine(),
    ): PairingViewModel = PairingViewModel(
        ensureLinkedIdentityUseCase = EnsureLinkedIdentityUseCase(pairingRepo),
        createPairingSessionUseCase = CreatePairingSessionUseCase(pairingRepo),
        redeemPairingCodeUseCase = RedeemPairingCodeUseCase(pairingRepo),
        listLinkedDevicesUseCase = ListLinkedDevicesUseCase(pairingRepo),
        revokeLinkedDeviceUseCase = RevokeLinkedDeviceUseCase(pairingRepo),
        syncEngine = syncEngine,
    )

    private class FakePairingRepo(
        private val session: PairingSession = PairingSession("123456", "2026-03-18T10:00:00"),
        private val devices: List<LinkedDevice> = emptyList(),
        private val shouldFail: Boolean = false,
    ) : PairingRepository {
        override suspend fun ensureLinkedIdentity() = Unit
        override suspend fun createPairingSession(ttlMinutes: Int): PairingSession {
            if (shouldFail) error("create error")
            return session
        }
        override suspend fun redeemPairingCode(code: String) {
            if (shouldFail) error("redeem error")
        }
        override suspend fun listLinkedDevices(): List<LinkedDevice> = devices
        override suspend fun revokeLinkedDevice(deviceId: String, reason: String?): Boolean {
            if (shouldFail) error("revoke error")
            return true
        }
    }

    private class FakeSyncEngine(private val shouldFail: Boolean = false) : SyncEngine {
        override val state = MutableStateFlow(SyncState())
        override suspend fun runOnce() {
            if (shouldFail) error("sync error")
        }
    }
}

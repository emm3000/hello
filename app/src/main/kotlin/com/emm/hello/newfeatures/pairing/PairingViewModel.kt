package com.emm.hello.newfeatures.pairing

import androidx.lifecycle.viewModelScope
import com.emm.data.sync.SyncRuntimePolicy
import com.emm.domain.sync.CreatePairingSessionUseCase
import com.emm.domain.sync.EnsureLinkedIdentityUseCase
import com.emm.domain.sync.ListLinkedDevicesUseCase
import com.emm.domain.sync.RedeemPairingCodeUseCase
import com.emm.domain.sync.RevokeLinkedDeviceUseCase
import com.emm.domain.sync.SyncEngine
import com.emm.hello.core.mvi.MviViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAIRING_CODE_LENGTH = 6

class PairingViewModel(
    private val ensureLinkedIdentityUseCase: EnsureLinkedIdentityUseCase,
    private val createPairingSessionUseCase: CreatePairingSessionUseCase,
    private val redeemPairingCodeUseCase: RedeemPairingCodeUseCase,
    private val listLinkedDevicesUseCase: ListLinkedDevicesUseCase,
    private val revokeLinkedDeviceUseCase: RevokeLinkedDeviceUseCase,
    private val syncEngine: SyncEngine,
    private val syncRuntimePolicy: SyncRuntimePolicy,
) : MviViewModel<PairingUiState, PairingUiIntent, PairingUiEffect>(
    initialState = PairingUiState(
        modeLabel = syncRuntimePolicy.modeLabel,
        remoteAvailable = syncRuntimePolicy.remoteEnabled,
    ),
) {

    init {
        if (syncRuntimePolicy.remoteEnabled) {
            onIntent(PairingUiIntent.RefreshDevicesClicked)
        }
    }

    override fun onIntent(intent: PairingUiIntent) {
        when (intent) {
            is PairingUiIntent.JoinCodeChanged -> {
                mutableState.update {
                    it.copy(joinCode = intent.value.take(PAIRING_CODE_LENGTH).filter(Char::isDigit))
                }
            }
            PairingUiIntent.CreateCodeClicked -> createCode()
            PairingUiIntent.JoinWithCodeClicked -> joinWithCode()
            PairingUiIntent.RefreshDevicesClicked -> refreshDevices()
            is PairingUiIntent.RevokeDeviceClicked -> revokeDevice(intent.deviceId)
        }
    }

    private fun createCode() {
        if (!guardRemoteAction()) return
        viewModelScope.launch {
            mutableState.update { it.copy(isGeneratingCode = true) }
            runCatching {
                ensureLinkedIdentityUseCase()
                syncEngine.runOnce()
                createPairingSessionUseCase(ttlMinutes = 10)
            }.onSuccess { session ->
                mutableState.update {
                    it.copy(
                        isGeneratingCode = false,
                        generatedCode = session.code,
                        generatedCodeExpiresAt = session.expiresAt,
                    )
                }
                mutableEffect.send(PairingUiEffect.ShowMessage("Código generado"))
                refreshDevices()
            }.onFailure { error ->
                mutableState.update { it.copy(isGeneratingCode = false) }
                mutableEffect.send(PairingUiEffect.ShowMessage(error.message ?: "No se pudo generar código"))
            }
        }
    }

    private fun joinWithCode() {
        if (!guardRemoteAction()) return
        val code = mutableState.value.joinCode
        if (code.length != PAIRING_CODE_LENGTH) {
            viewModelScope.launch {
                mutableEffect.send(PairingUiEffect.ShowMessage("Ingresa un código válido de 6 dígitos"))
            }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isSubmittingJoin = true) }
            runCatching {
                redeemPairingCodeUseCase(code)
                syncEngine.runOnce()
            }.onSuccess {
                mutableState.update {
                    it.copy(
                        isSubmittingJoin = false,
                        joinCode = "",
                    )
                }
                mutableEffect.send(PairingUiEffect.ShowMessage("Dispositivo vinculado"))
                refreshDevices()
            }.onFailure { error ->
                mutableState.update { it.copy(isSubmittingJoin = false) }
                mutableEffect.send(PairingUiEffect.ShowMessage(error.message ?: "No se pudo vincular"))
            }
        }
    }

    private fun revokeDevice(deviceId: String) {
        if (!guardRemoteAction()) return
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            runCatching {
                ensureLinkedIdentityUseCase()
                revokeLinkedDeviceUseCase(deviceId = deviceId, reason = "revoked_from_app")
            }.onSuccess { revoked ->
                mutableState.update { it.copy(isLoading = false) }
                mutableEffect.send(
                    PairingUiEffect.ShowMessage(
                        if (revoked) "Dispositivo revocado" else "No se pudo revocar"
                    )
                )
                refreshDevices()
            }.onFailure { error ->
                mutableState.update { it.copy(isLoading = false) }
                mutableEffect.send(PairingUiEffect.ShowMessage(error.message ?: "Error al revocar"))
            }
        }
    }

    private fun refreshDevices() {
        refreshDevices(showUnavailableMessage = true)
    }

    private fun refreshDevices(showUnavailableMessage: Boolean) {
        if (!syncRuntimePolicy.remoteEnabled) {
            mutableState.update {
                it.copy(
                    isLoading = false,
                    modeLabel = syncRuntimePolicy.modeLabel,
                    remoteAvailable = false,
                )
            }
            if (showUnavailableMessage) {
                viewModelScope.launch {
                    mutableEffect.send(PairingUiEffect.ShowMessage(LOCAL_ONLY_MESSAGE))
                }
            }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            runCatching {
                ensureLinkedIdentityUseCase()
                listLinkedDevicesUseCase()
            }.onSuccess { devices ->
                mutableState.update { it.copy(isLoading = false, devices = devices) }
            }.onFailure { error ->
                mutableState.update { it.copy(isLoading = false) }
                mutableEffect.send(PairingUiEffect.ShowMessage(error.message ?: "No se pudo cargar dispositivos"))
            }
        }
    }

    private fun guardRemoteAction(): Boolean {
        if (syncRuntimePolicy.remoteEnabled) return true
        viewModelScope.launch {
            mutableEffect.send(PairingUiEffect.ShowMessage(LOCAL_ONLY_MESSAGE))
        }
        return false
    }
}

private const val LOCAL_ONLY_MESSAGE = "La vinculación remota está temporalmente fuera de servicio en modo local-only"

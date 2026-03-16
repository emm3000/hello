package com.emm.hello.newfeatures.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.sync.CreatePairingSessionUseCase
import com.emm.domain.sync.EnsureLinkedIdentityUseCase
import com.emm.domain.sync.ListLinkedDevicesUseCase
import com.emm.domain.sync.RedeemPairingCodeUseCase
import com.emm.domain.sync.RevokeLinkedDeviceUseCase
import com.emm.domain.sync.SyncEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PairingViewModel(
    private val ensureLinkedIdentityUseCase: EnsureLinkedIdentityUseCase,
    private val createPairingSessionUseCase: CreatePairingSessionUseCase,
    private val redeemPairingCodeUseCase: RedeemPairingCodeUseCase,
    private val listLinkedDevicesUseCase: ListLinkedDevicesUseCase,
    private val revokeLinkedDeviceUseCase: RevokeLinkedDeviceUseCase,
    private val syncEngine: SyncEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(PairingUiState())
    val uiState = _state.asStateFlow()

    private val _effect = Channel<PairingUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        onIntent(PairingUiIntent.RefreshDevicesClicked)
    }

    fun onIntent(intent: PairingUiIntent) {
        when (intent) {
            is PairingUiIntent.JoinCodeChanged -> {
                _state.update { it.copy(joinCode = intent.value.take(6).filter(Char::isDigit)) }
            }
            PairingUiIntent.CreateCodeClicked -> createCode()
            PairingUiIntent.JoinWithCodeClicked -> joinWithCode()
            PairingUiIntent.RefreshDevicesClicked -> refreshDevices()
            is PairingUiIntent.RevokeDeviceClicked -> revokeDevice(intent.deviceId)
        }
    }

    private fun createCode() {
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingCode = true) }
            runCatching {
                ensureLinkedIdentityUseCase()
                syncEngine.runOnce()
                createPairingSessionUseCase(ttlMinutes = 10)
            }.onSuccess { session ->
                _state.update {
                    it.copy(
                        isGeneratingCode = false,
                        generatedCode = session.code,
                        generatedCodeExpiresAt = session.expiresAt,
                    )
                }
                _effect.send(PairingUiEffect.ShowMessage("Código generado"))
                refreshDevices()
            }.onFailure { error ->
                _state.update { it.copy(isGeneratingCode = false) }
                _effect.send(
                    PairingUiEffect.ShowMessage(
                        error.message ?: "No se pudo generar código"
                    )
                )
            }
        }
    }

    private fun joinWithCode() {
        val code = _state.value.joinCode
        if (code.length != 6) {
            viewModelScope.launch {
                _effect.send(PairingUiEffect.ShowMessage("Ingresa un código válido de 6 dígitos"))
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingJoin = true) }
            runCatching {
                redeemPairingCodeUseCase(code)
                syncEngine.runOnce()
            }.onSuccess {
                _state.update {
                    it.copy(
                        isSubmittingJoin = false,
                        joinCode = "",
                    )
                }
                _effect.send(PairingUiEffect.ShowMessage("Dispositivo vinculado"))
                refreshDevices()
            }.onFailure { error ->
                _state.update { it.copy(isSubmittingJoin = false) }
                _effect.send(PairingUiEffect.ShowMessage(error.message ?: "No se pudo vincular"))
            }
        }
    }

    private fun revokeDevice(deviceId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                ensureLinkedIdentityUseCase()
                revokeLinkedDeviceUseCase(deviceId = deviceId, reason = "revoked_from_app")
            }.onSuccess { revoked ->
                _state.update { it.copy(isLoading = false) }
                _effect.send(
                    PairingUiEffect.ShowMessage(
                        if (revoked) "Dispositivo revocado" else "No se pudo revocar"
                    )
                )
                refreshDevices()
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false) }
                _effect.send(PairingUiEffect.ShowMessage(error.message ?: "Error al revocar"))
            }
        }
    }

    private fun refreshDevices() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                ensureLinkedIdentityUseCase()
                listLinkedDevicesUseCase()
            }.onSuccess { devices ->
                _state.update { it.copy(isLoading = false, devices = devices) }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false) }
                _effect.send(PairingUiEffect.ShowMessage(error.message ?: "No se pudo cargar dispositivos"))
            }
        }
    }
}

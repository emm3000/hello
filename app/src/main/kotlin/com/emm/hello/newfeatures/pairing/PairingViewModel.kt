package com.emm.hello.newfeatures.pairing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.sync.CreatePairingSessionUseCase
import com.emm.domain.sync.EnsureLinkedIdentityUseCase
import com.emm.domain.sync.ListLinkedDevicesUseCase
import com.emm.domain.sync.RedeemPairingCodeUseCase
import com.emm.domain.sync.RevokeLinkedDeviceUseCase
import com.emm.domain.sync.SyncEngine
import kotlinx.coroutines.launch

class PairingViewModel(
    private val ensureLinkedIdentityUseCase: EnsureLinkedIdentityUseCase,
    private val createPairingSessionUseCase: CreatePairingSessionUseCase,
    private val redeemPairingCodeUseCase: RedeemPairingCodeUseCase,
    private val listLinkedDevicesUseCase: ListLinkedDevicesUseCase,
    private val revokeLinkedDeviceUseCase: RevokeLinkedDeviceUseCase,
    private val syncEngine: SyncEngine,
) : ViewModel() {

    var state by mutableStateOf(PairingUiState())
        private set

    init {
        refreshDevices()
    }

    fun onJoinCodeChange(value: String) {
        state = state.copy(joinCode = value.take(6).filter { it.isDigit() }, error = null, success = null)
    }

    fun createCode() {
        viewModelScope.launch {
            state = state.copy(isGeneratingCode = true, error = null, success = null)
            runCatching {
                ensureLinkedIdentityUseCase.execute()
                syncEngine.runOnce()
                createPairingSessionUseCase.execute(ttlMinutes = 10)
            }.onSuccess { session ->
                state = state.copy(
                    isGeneratingCode = false,
                    generatedCode = session.code,
                    generatedCodeExpiresAt = session.expiresAt,
                    success = "Código generado",
                )
                refreshDevices()
            }.onFailure { error ->
                state = state.copy(isGeneratingCode = false, error = error.message ?: "No se pudo generar código")
            }
        }
    }

    fun joinWithCode() {
        val code = state.joinCode
        if (code.length != 6) {
            state = state.copy(error = "Ingresa un código válido de 6 dígitos")
            return
        }
        viewModelScope.launch {
            state = state.copy(isSubmittingJoin = true, error = null, success = null)
            runCatching {
                redeemPairingCodeUseCase.execute(code)
                syncEngine.runOnce()
            }.onSuccess {
                state = state.copy(
                    isSubmittingJoin = false,
                    joinCode = "",
                    success = "Dispositivo vinculado",
                )
                refreshDevices()
            }.onFailure { error ->
                state = state.copy(isSubmittingJoin = false, error = error.message ?: "No se pudo vincular")
            }
        }
    }

    fun revokeDevice(deviceId: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null, success = null)
            runCatching {
                ensureLinkedIdentityUseCase.execute()
                revokeLinkedDeviceUseCase.execute(deviceId = deviceId, reason = "revoked_from_app")
            }.onSuccess { revoked ->
                state = state.copy(
                    isLoading = false,
                    success = if (revoked) "Dispositivo revocado" else "No se pudo revocar",
                )
                refreshDevices()
            }.onFailure { error ->
                state = state.copy(isLoading = false, error = error.message ?: "Error al revocar")
            }
        }
    }

    fun refreshDevices() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            runCatching {
                ensureLinkedIdentityUseCase.execute()
                listLinkedDevicesUseCase.execute()
            }.onSuccess { devices ->
                state = state.copy(isLoading = false, devices = devices)
            }.onFailure { error ->
                state = state.copy(isLoading = false, error = error.message ?: "No se pudo cargar dispositivos")
            }
        }
    }
}

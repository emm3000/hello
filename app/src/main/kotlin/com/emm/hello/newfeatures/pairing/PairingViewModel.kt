package com.emm.hello.newfeatures.pairing

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.sync.SupabaseSyncRemoteDataSource
import com.emm.domain.sync.SyncEngine
import kotlinx.coroutines.launch
import java.time.Instant

class PairingViewModel(
    private val remote: SupabaseSyncRemoteDataSource,
    private val localDeviceIdentityProvider: LocalDeviceIdentityProvider,
    private val db: HelloDb,
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
                ensureLinkedIdentity()
                syncEngine.runOnce()
                remote.createPairingSession(ttlMinutes = 10)
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
                val deviceId = localDeviceIdentityProvider.getOrCreateDeviceId()
                remote.ensureAnonymousSession()
                val redeem = remote.redeemPairingCode(
                    code = code,
                    deviceId = deviceId,
                    deviceName = Build.MODEL,
                    platform = "android",
                )
                persistLocalAccountState(
                    appAccountId = redeem.appAccountId,
                    authUserId = redeem.authUserId,
                    resetSyncCursor = true,
                )
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
                ensureLinkedIdentity()
                remote.revokeLinkedDevice(deviceId = deviceId, reason = "revoked_from_app")
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
                ensureLinkedIdentity()
                remote.listLinkedDevices()
            }.onSuccess { devices ->
                state = state.copy(isLoading = false, devices = devices)
            }.onFailure { error ->
                state = state.copy(isLoading = false, error = error.message ?: "No se pudo cargar dispositivos")
            }
        }
    }

    private suspend fun ensureLinkedIdentity() {
        val deviceId = localDeviceIdentityProvider.getOrCreateDeviceId()
        remote.ensureAnonymousSession()
        val bootstrap = remote.bootstrapAnonymousDevice(
            deviceId = deviceId,
            deviceName = Build.MODEL,
            platform = "android",
        )
        persistLocalAccountState(
            appAccountId = bootstrap.appAccountId,
            authUserId = bootstrap.authUserId,
            resetSyncCursor = false,
        )
    }

    private fun persistLocalAccountState(
        appAccountId: String,
        authUserId: String,
        resetSyncCursor: Boolean,
    ) {
        val queries = db.localFirstQueries
        val now = Instant.now().toEpochMilli()
        val current = queries.selectLocalAccountState().executeAsOneOrNull()
        queries.upsertLocalAccountState(
            appAccountId = appAccountId,
            authUserId = authUserId,
            pairingState = "Paired",
            createdAt = current?.createdAt ?: now,
            updatedAt = now,
        )

        if (resetSyncCursor) {
            val checkpoint = queries.selectSyncCheckpoint().executeAsOneOrNull()
            queries.upsertSyncCheckpoint(
                lastPulledCursor = 0L,
                lastSuccessfulSyncAt = checkpoint?.lastSuccessfulSyncAt,
                lastSyncError = null,
                lastSyncErrorAt = null,
                updatedAt = now,
            )
        }
    }
}

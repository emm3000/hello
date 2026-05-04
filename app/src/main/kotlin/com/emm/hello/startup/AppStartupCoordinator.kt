package com.emm.hello.startup

import com.emm.data.sync.LocalIdentityInitializer
import com.emm.data.sync.SyncRuntimePolicy
import com.emm.domain.sync.EnsureLinkedIdentityUseCase
import com.emm.hello.logging.logError
import com.emm.hello.logging.logInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppStartupCoordinator(
    private val ensureLinkedIdentityUseCase: EnsureLinkedIdentityUseCase,
    private val localIdentityInitializer: LocalIdentityInitializer,
    private val syncRuntimePolicy: SyncRuntimePolicy,
    private val syncRuntimeController: SyncRuntimeController,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val startMutex = Mutex()
    private val mutableState = MutableStateFlow<AppStartupState>(AppStartupState.Initializing)

    val state: StateFlow<AppStartupState> = mutableState.asStateFlow()

    fun start() {
        scope.launch {
            startMutex.withLock {
                if (mutableState.value is AppStartupState.Ready) return@withLock
                mutableState.value = AppStartupState.Initializing
                runCatching {
                    logInfo(TAG, "start:ensure_local_identity")
                    localIdentityInitializer.ensureReady()
                    if (syncRuntimePolicy.remoteEnabled) {
                        logInfo(TAG, "start:ensure_remote_identity")
                        ensureLinkedIdentityUseCase()
                        logInfo(TAG, "start:enable_remote_runtime")
                        syncRuntimeController.start()
                    } else {
                        logInfo(TAG, "start:disable_remote_runtime")
                        syncRuntimeController.stop()
                    }
                }.onSuccess {
                    logInfo(TAG, "start:ready")
                    mutableState.value = AppStartupState.Ready(
                        isLocalOnly = !syncRuntimePolicy.remoteEnabled,
                        modeLabel = syncRuntimePolicy.modeLabel,
                    )
                }.onFailure { error ->
                    logError(TAG, "start:error ${error.message}", error)
                    mutableState.value = AppStartupState.Error(
                        message = error.message ?: "No se pudo inicializar la app.",
                    )
                }
            }
        }
    }
}

sealed interface AppStartupState {
    data object Initializing : AppStartupState
    data class Ready(
        val isLocalOnly: Boolean,
        val modeLabel: String,
    ) : AppStartupState
    data class Error(val message: String) : AppStartupState
}

private const val TAG = "AppStartup"

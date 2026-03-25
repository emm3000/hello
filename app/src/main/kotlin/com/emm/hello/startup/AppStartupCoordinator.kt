package com.emm.hello.startup

import android.content.Context
import com.emm.domain.sync.EnsureLinkedIdentityUseCase
import com.emm.hello.logging.logError
import com.emm.hello.logging.logInfo
import com.emm.hello.sync.PendingOperationsSyncScheduler
import com.emm.hello.sync.Sync
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
    private val appContext: Context,
    private val ensureLinkedIdentityUseCase: EnsureLinkedIdentityUseCase,
    private val pendingOperationsSyncScheduler: PendingOperationsSyncScheduler,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val startMutex = Mutex()
    private var syncInfraStarted = false
    private val mutableState = MutableStateFlow<AppStartupState>(AppStartupState.Initializing)

    val state: StateFlow<AppStartupState> = mutableState.asStateFlow()

    fun start() {
        scope.launch {
            startMutex.withLock {
                if (mutableState.value is AppStartupState.Ready) return@withLock
                mutableState.value = AppStartupState.Initializing
                runCatching {
                    logInfo(TAG, "start:ensure_identity")
                    ensureLinkedIdentityUseCase()
                    if (!syncInfraStarted) {
                        logInfo(TAG, "start:initialize_sync")
                        Sync.initialize(appContext)
                        pendingOperationsSyncScheduler.start()
                        syncInfraStarted = true
                    }
                }.onSuccess {
                    logInfo(TAG, "start:ready")
                    mutableState.value = AppStartupState.Ready
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
    data object Ready : AppStartupState
    data class Error(val message: String) : AppStartupState
}

private const val TAG = "AppStartup"

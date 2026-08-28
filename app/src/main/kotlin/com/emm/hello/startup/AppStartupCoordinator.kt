package com.emm.hello.startup

import com.emm.domain.localfirst.LocalIdentityInitializer
import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.domain.seed.SeedDataInitializer
import com.emm.hello.logging.logError
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
    private val localIdentityInitializer: LocalIdentityInitializer,
    private val seedDataInitializer: SeedDataInitializer,
    private val onboardingStateRepository: OnboardingStateRepository,
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
                    localIdentityInitializer.ensureReady()
                    seedDataInitializer.ensureSeeded()
                }.onSuccess {
                    mutableState.value = AppStartupState.Ready(
                        hasSeenWelcome = onboardingStateRepository.hasSeenWelcome(),
                    )
                }.onFailure { error ->
                    logError(TAG, "start:error ${error.message}", error)
                    mutableState.value = AppStartupState.Error(
                        message = "Couldn't prepare the app's local mode.",
                    )
                }
            }
        }
    }
}

sealed interface AppStartupState {
    data object Initializing : AppStartupState
    data class Ready(val hasSeenWelcome: Boolean) : AppStartupState
    data class Error(val message: String) : AppStartupState
}

private const val TAG = "AppStartup"

package com.emm.hello.startup

import com.emm.domain.authoring.FindPendingEnrichmentsUseCase
import com.emm.domain.ids.FlashcardId
import com.emm.domain.localfirst.LocalIdentityInitializer
import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.domain.seed.SeedDataInitializer
import com.emm.hello.logging.logError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class AppStartupCoordinator(
    private val localIdentityInitializer: LocalIdentityInitializer,
    private val seedDataInitializer: SeedDataInitializer,
    private val onboardingStateRepository: OnboardingStateRepository,
    private val findPendingEnrichments: FindPendingEnrichmentsUseCase,
    private val requeueEnrichments: (List<FlashcardId>) -> Unit,
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
                    withTimeout(STARTUP_TIMEOUT_MS) {
                        localIdentityInitializer.ensureReady()
                        seedDataInitializer.ensureSeeded()
                    }
                }.onSuccess {
                    mutableState.value = AppStartupState.Ready(
                        hasSeenWelcome = onboardingStateRepository.hasSeenWelcome(),
                    )
                    requeuePendingEnrichments()
                }.onFailure { error ->
                    logError(TAG, "start:error ${error.message}", error)
                    mutableState.value = AppStartupState.Error(message = error.toStartupMessage())
                }
            }
        }
    }

    private suspend fun requeuePendingEnrichments() {
        runCatching {
            val pending: List<FlashcardId> = findPendingEnrichments()
            if (pending.isNotEmpty()) requeueEnrichments(pending)
        }.onFailure { error -> logError(TAG, "requeue:error ${error.message}", error) }
    }
}

private fun Throwable.toStartupMessage(): String = when (this) {
    is TimeoutCancellationException -> "The app took too long to start."
    else -> "Couldn't prepare the app's local mode."
}

sealed interface AppStartupState {
    data object Initializing : AppStartupState
    data class Ready(val hasSeenWelcome: Boolean) : AppStartupState
    data class Error(val message: String) : AppStartupState
}

internal const val STARTUP_TIMEOUT_MS: Long = 5_000L
private const val TAG = "AppStartup"

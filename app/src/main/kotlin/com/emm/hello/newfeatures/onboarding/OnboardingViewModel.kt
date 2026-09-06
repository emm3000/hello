package com.emm.hello.newfeatures.onboarding

import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.notifications.NotificationPermission

class OnboardingViewModel(
    private val onboardingState: OnboardingStateRepository,
    private val notificationPermission: NotificationPermission,
) : MviViewModel<OnboardingUiState, OnboardingUiIntent, OnboardingUiEffect>(
    initialState = OnboardingUiState,
) {

    override fun onIntent(intent: OnboardingUiIntent) {
        when (intent) {
            is OnboardingUiIntent.StartClicked -> startLearning()
            is OnboardingUiIntent.NotificationPermissionSettled -> enterApp()
            is OnboardingUiIntent.BackPressed -> sendEffect(OnboardingUiEffect.CloseOnboarding)
        }
    }

    private fun startLearning() {
        if (notificationPermission.isGranted()) {
            enterApp()
        } else {
            sendEffect(OnboardingUiEffect.RequestNotificationPermission)
        }
    }

    private fun enterApp() {
        onboardingState.markWelcomeSeen()
        sendEffect(OnboardingUiEffect.NavigateToToday)
    }
}

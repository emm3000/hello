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
            is OnboardingUiIntent.NotificationPermissionSettled -> sendEffect(OnboardingUiEffect.NavigateToToday)
            is OnboardingUiIntent.BackPressed -> sendEffect(OnboardingUiEffect.CloseOnboarding)
        }
    }

    private fun startLearning() {
        onboardingState.markWelcomeSeen()
        if (notificationPermission.isGranted()) {
            sendEffect(OnboardingUiEffect.NavigateToToday)
        } else {
            sendEffect(OnboardingUiEffect.RequestNotificationPermission)
        }
    }
}

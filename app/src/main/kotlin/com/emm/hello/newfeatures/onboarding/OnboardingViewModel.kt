package com.emm.hello.newfeatures.onboarding

import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.hello.analytics.ProductAnalytics
import com.emm.hello.analytics.ProductEvent
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.notifications.NotificationPermission

class OnboardingViewModel(
    private val onboardingState: OnboardingStateRepository,
    private val notificationPermission: NotificationPermission,
    private val productAnalytics: ProductAnalytics,
) : MviViewModel<OnboardingUiState, OnboardingUiIntent, OnboardingUiEffect>(
    initialState = OnboardingUiState,
) {

    override fun onIntent(intent: OnboardingUiIntent) {
        when (intent) {
            is OnboardingUiIntent.StartClicked -> startLearning()
            is OnboardingUiIntent.NotificationPermissionSettled -> sendEffect(OnboardingUiEffect.NavigateToToday)
        }
    }

    private fun startLearning() {
        onboardingState.markWelcomeSeen()
        productAnalytics.track(ProductEvent.OnboardingCompleted)
        if (notificationPermission.isGranted()) {
            sendEffect(OnboardingUiEffect.NavigateToToday)
        } else {
            sendEffect(OnboardingUiEffect.RequestNotificationPermission)
        }
    }
}

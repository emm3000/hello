package com.emm.hello.newfeatures.onboarding

import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.hello.core.mvi.MviViewModel

class OnboardingViewModel(
    private val onboardingState: OnboardingStateRepository,
) : MviViewModel<OnboardingUiState, OnboardingUiIntent, OnboardingUiEffect>(
    initialState = OnboardingUiState,
) {

    override fun onIntent(intent: OnboardingUiIntent) {
        when (intent) {
            is OnboardingUiIntent.StartClicked -> startLearning()
            is OnboardingUiIntent.BackPressed -> sendEffect(OnboardingUiEffect.CloseOnboarding)
        }
    }

    private fun startLearning() {
        onboardingState.markWelcomeSeen()
        sendEffect(OnboardingUiEffect.NavigateToToday)
    }
}

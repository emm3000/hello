package com.emm.hello.newfeatures.onboarding

import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.hello.core.mvi.MviViewModel

class OnboardingViewModel(
    private val onboardingState: OnboardingStateRepository,
) : MviViewModel<OnboardingUiState, OnboardingUiIntent, OnboardingUiEffect>(
    initialState = OnboardingUiState(),
) {

    override fun onIntent(intent: OnboardingUiIntent) {
        when (intent) {
            is OnboardingUiIntent.PageChanged -> onPageChanged(intent.page)
            is OnboardingUiIntent.NextClicked -> onNextClicked()
            is OnboardingUiIntent.SkipClicked -> finishOnboarding()
            is OnboardingUiIntent.FinishClicked -> finishOnboarding()
        }
    }

    private fun onPageChanged(page: Int) {
        setState { copy(currentPage = page) }
    }

    private fun onNextClicked() {
        if (currentState.isLastPage) {
            finishOnboarding()
        } else {
            val nextPage = currentState.currentPage + 1
            sendEffect(OnboardingUiEffect.ScrollToPage(nextPage))
        }
    }

    private fun finishOnboarding() {
        onboardingState.markWelcomeSeen()
        sendEffect(OnboardingUiEffect.NavigateToDashboard)
    }
}

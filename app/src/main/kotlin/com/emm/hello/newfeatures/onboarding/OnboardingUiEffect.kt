package com.emm.hello.newfeatures.onboarding

import com.emm.hello.core.mvi.MviEffect

sealed interface OnboardingUiEffect : MviEffect {
    data class ScrollToPage(val page: Int) : OnboardingUiEffect
    data object NavigateToHoy : OnboardingUiEffect
    data object CloseOnboarding : OnboardingUiEffect
}

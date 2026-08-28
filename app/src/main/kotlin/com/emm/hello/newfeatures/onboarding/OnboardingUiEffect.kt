package com.emm.hello.newfeatures.onboarding

import com.emm.hello.core.mvi.MviEffect

sealed interface OnboardingUiEffect : MviEffect {
    data object NavigateToToday : OnboardingUiEffect
    data object CloseOnboarding : OnboardingUiEffect
}

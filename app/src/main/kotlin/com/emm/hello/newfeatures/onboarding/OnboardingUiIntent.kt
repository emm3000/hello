package com.emm.hello.newfeatures.onboarding

import com.emm.hello.core.mvi.MviIntent

sealed interface OnboardingUiIntent : MviIntent {
    data class PageChanged(val page: Int) : OnboardingUiIntent
    data object NextClicked : OnboardingUiIntent
    data object SkipClicked : OnboardingUiIntent
    data object FinishClicked : OnboardingUiIntent
    data object BackPressed : OnboardingUiIntent
}

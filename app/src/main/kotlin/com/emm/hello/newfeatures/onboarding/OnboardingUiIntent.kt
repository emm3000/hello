package com.emm.hello.newfeatures.onboarding

import com.emm.hello.core.mvi.MviIntent

sealed interface OnboardingUiIntent : MviIntent {
    data object StartClicked : OnboardingUiIntent
    data object NotificationPermissionSettled : OnboardingUiIntent
    data object BackPressed : OnboardingUiIntent
}

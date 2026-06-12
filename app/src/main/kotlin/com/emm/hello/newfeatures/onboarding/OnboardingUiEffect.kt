package com.emm.hello.newfeatures.onboarding

import com.emm.hello.core.mvi.MviEffect

/**
 * One-shot effects emitted by [OnboardingViewModel].
 *
 * - [ScrollToPage] — tells the Route to animate the pager to the given page index
 * - [NavigateToDashboard] — tells the Route to replace the back stack with Dashboard
 */
sealed interface OnboardingUiEffect : MviEffect {
    data class ScrollToPage(val page: Int) : OnboardingUiEffect
    data object NavigateToDashboard : OnboardingUiEffect
}

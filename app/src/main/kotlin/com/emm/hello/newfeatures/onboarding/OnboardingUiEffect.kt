package com.emm.hello.newfeatures.onboarding

import com.emm.hello.core.mvi.MviEffect

/**
 * One-shot effects emitted by [OnboardingViewModel].
 *
 * - [ScrollToPage] — tells the Route to animate the pager to the given page index
 * - [NavigateToHoy] — tells the Route to replace the back stack with Hoy
 * - [CloseOnboarding] — tells the Route to navigate back (system back on first slide)
 */
sealed interface OnboardingUiEffect : MviEffect {
    data class ScrollToPage(val page: Int) : OnboardingUiEffect
    data object NavigateToHoy : OnboardingUiEffect
    data object CloseOnboarding : OnboardingUiEffect
}

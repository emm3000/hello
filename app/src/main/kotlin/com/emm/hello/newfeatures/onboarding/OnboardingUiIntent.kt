package com.emm.hello.newfeatures.onboarding

import com.emm.hello.core.mvi.MviIntent

/**
 * User intents for the onboarding carousel.
 *
 * Names describe what the user did (past-tense / noun-verb per AGENTS.md):
 * - [PageChanged] — pager swipe settled on a new page index
 * - [NextClicked] — primary CTA tapped (advances on non-last page, finishes on last)
 * - [SkipClicked] — skip action tapped from any page
 * - [FinishClicked] — explicit "Empezar" CTA on the last page
 */
sealed interface OnboardingUiIntent : MviIntent {
    data class PageChanged(val page: Int) : OnboardingUiIntent
    data object NextClicked : OnboardingUiIntent
    data object SkipClicked : OnboardingUiIntent
    data object FinishClicked : OnboardingUiIntent
}

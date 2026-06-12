package com.emm.hello.newfeatures.onboarding

import com.emm.hello.core.mvi.MviState

/**
 * MVI state for the onboarding carousel screen.
 *
 * [pages] is static — always the full ordered list from [OnboardingPage].
 * [currentPage] is the zero-based index of the visible page.
 * [isLastPage] is derived; true only when the user is on the final page.
 */
data class OnboardingUiState(
    val pages: List<OnboardingPage> = OnboardingPage.all,
    val currentPage: Int = 0,
) : MviState {
    val isLastPage: Boolean
        get() = currentPage == pages.lastIndex
}

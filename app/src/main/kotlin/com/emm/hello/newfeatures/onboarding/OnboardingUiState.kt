package com.emm.hello.newfeatures.onboarding

import com.emm.hello.core.mvi.MviState

data class OnboardingUiState(
    val pages: List<OnboardingPage> = OnboardingPage.entries,
    val currentPage: Int = 0,
) : MviState {
    val isLastPage: Boolean
        get() = currentPage == pages.lastIndex
}

package com.emm.hello.newfeatures.onboarding

import androidx.annotation.StringRes
import com.emm.hello.R

/**
 * Ordered list of pages in the onboarding carousel.
 * Each entry carries its title and body string resource ids plus an illustration key.
 */
enum class OnboardingPage(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val illustration: OnboardingIllustration,
) {
    Decks(
        titleRes = R.string.onboarding_decks_title,
        bodyRes = R.string.onboarding_decks_body,
        illustration = OnboardingIllustration.Decks,
    ),
    SpacedRepetition(
        titleRes = R.string.onboarding_srs_title,
        bodyRes = R.string.onboarding_srs_body,
        illustration = OnboardingIllustration.SpacedRepetition,
    ),
    Grading(
        titleRes = R.string.onboarding_grading_title,
        bodyRes = R.string.onboarding_grading_body,
        illustration = OnboardingIllustration.Grading,
    ),
}

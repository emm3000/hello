package com.emm.hello.newfeatures.onboarding

import androidx.annotation.DrawableRes
import com.emm.hello.R

/**
 * Illustration key for each onboarding page, backed by a transparent drawable.
 */
enum class OnboardingIllustration(@DrawableRes val drawableRes: Int) {
    Decks(R.drawable.onboarding_decks),
    SpacedRepetition(R.drawable.onboarding_spaced),
    Grading(R.drawable.onboarding_grading),
}

package com.emm.domain.onboarding

interface OnboardingStateRepository {

    fun hasSeenWelcome(): Boolean

    fun markWelcomeSeen()

    fun hasSeenGradeHint(): Boolean

    fun markGradeHintSeen()
}

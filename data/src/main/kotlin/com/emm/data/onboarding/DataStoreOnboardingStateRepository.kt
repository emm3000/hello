package com.emm.data.onboarding

import com.emm.data.remote.DataStore
import com.emm.domain.onboarding.OnboardingStateRepository

class DataStoreOnboardingStateRepository(
    private val dataStore: DataStore,
) : OnboardingStateRepository {

    override fun hasSeenWelcome(): Boolean = dataStore.hasSeenOnboarding

    override fun markWelcomeSeen() {
        dataStore.hasSeenOnboarding = true
    }

    override fun hasSeenGradeHint(): Boolean = dataStore.hasSeenGradeHint

    override fun markGradeHintSeen() {
        dataStore.hasSeenGradeHint = true
    }
}

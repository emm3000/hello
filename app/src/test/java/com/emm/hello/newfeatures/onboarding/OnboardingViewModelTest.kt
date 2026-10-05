package com.emm.hello.newfeatures.onboarding

import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.hello.MainDispatcherRule
import com.emm.hello.notifications.NotificationPermission
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `StartClicked with permission granted marks welcome seen and emits NavigateToToday`() = runTest {
        val repo = FakeOnboardingStateRepository()
        val viewModel = buildViewModel(repo, FakeNotificationPermission(granted = true))

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.StartClicked)
        val effect = effectDeferred.await()

        assertThat(effect).isEqualTo(OnboardingUiEffect.NavigateToToday)
        assertThat(repo.welcomeSeenCalled).isTrue()
    }

    @Test
    fun `StartClicked without permission marks welcome seen and emits RequestNotificationPermission`() = runTest {
        val repo = FakeOnboardingStateRepository()
        val viewModel = buildViewModel(repo, FakeNotificationPermission(granted = false))

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.StartClicked)
        val effect = effectDeferred.await()

        assertThat(effect).isEqualTo(OnboardingUiEffect.RequestNotificationPermission)
        assertThat(repo.welcomeSeenCalled).isTrue()
    }

    @Test
    fun `NotificationPermissionSettled emits NavigateToToday without marking welcome seen`() = runTest {
        val repo = FakeOnboardingStateRepository()
        val viewModel = buildViewModel(repo, FakeNotificationPermission(granted = false))

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.NotificationPermissionSettled)
        val effect = effectDeferred.await()

        assertThat(effect).isEqualTo(OnboardingUiEffect.NavigateToToday)
        assertThat(repo.welcomeSeenCalled).isFalse()
    }

    private fun buildViewModel(
        repo: OnboardingStateRepository = FakeOnboardingStateRepository(),
        notificationPermission: NotificationPermission = FakeNotificationPermission(),
    ) = OnboardingViewModel(onboardingState = repo, notificationPermission = notificationPermission)
}

private class FakeOnboardingStateRepository : OnboardingStateRepository {
    var welcomeSeen: Boolean = false
    var welcomeSeenCalled: Boolean = false

    override fun hasSeenWelcome(): Boolean = welcomeSeen

    override fun markWelcomeSeen() {
        welcomeSeenCalled = true
        welcomeSeen = true
    }
}

private class FakeNotificationPermission(var granted: Boolean = true) : NotificationPermission {

    override fun isGranted(): Boolean = granted
}

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
    fun `StartClicked calls markWelcomeSeen`() = runTest {
        val repo = FakeOnboardingStateRepository()
        val viewModel = buildViewModel(repo)

        backgroundScope.async { viewModel.effect.first() }.also {
            viewModel.onIntent(OnboardingUiIntent.StartClicked)
            it.await()
        }

        assertThat(repo.welcomeSeenCalled).isTrue()
    }

    @Test
    fun `StartClicked emits NavigateToToday`() = runTest {
        val viewModel = buildViewModel()

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.StartClicked)

        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(OnboardingUiEffect.NavigateToToday)
    }

    @Test
    fun `markWelcomeSeen is recorded before NavigateToToday on StartClicked`() = runTest {
        val repo = FakeOnboardingStateRepository()
        val viewModel = buildViewModel(repo)

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.StartClicked)
        val effect = effectDeferred.await()

        assertThat(effect).isEqualTo(OnboardingUiEffect.NavigateToToday)
        assertThat(repo.welcomeSeenCalled).isTrue()
    }

    @Test
    fun `StartClicked without notification permission emits RequestNotificationPermission`() = runTest {
        val viewModel = buildViewModel(notificationPermission = FakeNotificationPermission(granted = false))

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.StartClicked)

        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(OnboardingUiEffect.RequestNotificationPermission)
    }

    @Test
    fun `StartClicked without notification permission does not call markWelcomeSeen`() = runTest {
        val repo = FakeOnboardingStateRepository()
        val viewModel = buildViewModel(repo, FakeNotificationPermission(granted = false))

        backgroundScope.async { viewModel.effect.first() }.also {
            viewModel.onIntent(OnboardingUiIntent.StartClicked)
            it.await()
        }

        assertThat(repo.welcomeSeenCalled).isFalse()
    }

    @Test
    fun `NotificationPermissionSettled calls markWelcomeSeen`() = runTest {
        val repo = FakeOnboardingStateRepository()
        val viewModel = buildViewModel(repo, FakeNotificationPermission(granted = false))

        backgroundScope.async { viewModel.effect.first() }.also {
            viewModel.onIntent(OnboardingUiIntent.NotificationPermissionSettled)
            it.await()
        }

        assertThat(repo.welcomeSeenCalled).isTrue()
    }

    @Test
    fun `NotificationPermissionSettled emits NavigateToToday even when permission is denied`() = runTest {
        val viewModel = buildViewModel(notificationPermission = FakeNotificationPermission(granted = false))

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.NotificationPermissionSettled)

        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(OnboardingUiEffect.NavigateToToday)
    }

    @Test
    fun `markWelcomeSeen is recorded before NavigateToToday on NotificationPermissionSettled`() = runTest {
        val repo = FakeOnboardingStateRepository()
        val viewModel = buildViewModel(repo, FakeNotificationPermission(granted = false))

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.NotificationPermissionSettled)
        val effect = effectDeferred.await()

        assertThat(effect).isEqualTo(OnboardingUiEffect.NavigateToToday)
        assertThat(repo.welcomeSeenCalled).isTrue()
    }

    @Test
    fun `BackPressed emits CloseOnboarding`() = runTest {
        val viewModel = buildViewModel()

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.BackPressed)

        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(OnboardingUiEffect.CloseOnboarding)
    }

    @Test
    fun `BackPressed does not call markWelcomeSeen`() = runTest {
        val repo = FakeOnboardingStateRepository()
        val viewModel = buildViewModel(repo)

        backgroundScope.async { viewModel.effect.first() }.also {
            viewModel.onIntent(OnboardingUiIntent.BackPressed)
            it.await()
        }

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

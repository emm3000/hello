package com.emm.hello.newfeatures.onboarding

import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.hello.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── PageChanged ──────────────────────────────────────────────────────────

    @Test
    fun `PageChanged updates currentPage in state`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onIntent(OnboardingUiIntent.PageChanged(2))

        assertThat(viewModel.state.value.currentPage).isEqualTo(2)
    }

    @Test
    fun `PageChanged to first page keeps isLastPage false`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onIntent(OnboardingUiIntent.PageChanged(0))

        assertThat(viewModel.state.value.isLastPage).isFalse()
    }

    @Test
    fun `PageChanged to last page sets isLastPage true`() = runTest {
        val viewModel = buildViewModel()
        val lastIndex = OnboardingPage.all.lastIndex

        viewModel.onIntent(OnboardingUiIntent.PageChanged(lastIndex))

        assertThat(viewModel.state.value.isLastPage).isTrue()
    }

    // ── NextClicked on non-last page ─────────────────────────────────────────

    @Test
    fun `NextClicked on page 0 emits ScrollToPage(1)`() = runTest {
        val viewModel = buildViewModel()

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.PageChanged(0))
        viewModel.onIntent(OnboardingUiIntent.NextClicked)

        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(OnboardingUiEffect.ScrollToPage(1))
    }

    @Test
    fun `NextClicked on page 1 emits ScrollToPage(2)`() = runTest {
        val viewModel = buildViewModel()

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.PageChanged(1))
        viewModel.onIntent(OnboardingUiIntent.NextClicked)

        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(OnboardingUiEffect.ScrollToPage(2))
    }

    // ── NextClicked on last page ─────────────────────────────────────────────

    @Test
    fun `NextClicked on last page calls markWelcomeSeen`() = runTest {
        val repo = FakeOnboardingStateRepository()
        val viewModel = buildViewModel(repo)
        val lastIndex = OnboardingPage.all.lastIndex

        viewModel.onIntent(OnboardingUiIntent.PageChanged(lastIndex))
        backgroundScope.async { viewModel.effect.first() }.also {
            viewModel.onIntent(OnboardingUiIntent.NextClicked)
            it.await()
        }

        assertThat(repo.welcomeSeenCalled).isTrue()
    }

    @Test
    fun `NextClicked on last page emits NavigateToDashboard`() = runTest {
        val viewModel = buildViewModel()
        val lastIndex = OnboardingPage.all.lastIndex

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.PageChanged(lastIndex))
        viewModel.onIntent(OnboardingUiIntent.NextClicked)

        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(OnboardingUiEffect.NavigateToDashboard)
    }

    // ── SkipClicked ──────────────────────────────────────────────────────────

    @Test
    fun `SkipClicked from page 0 calls markWelcomeSeen`() = runTest {
        val repo = FakeOnboardingStateRepository()
        val viewModel = buildViewModel(repo)

        backgroundScope.async { viewModel.effect.first() }.also {
            viewModel.onIntent(OnboardingUiIntent.SkipClicked)
            it.await()
        }

        assertThat(repo.welcomeSeenCalled).isTrue()
    }

    @Test
    fun `SkipClicked from any page emits NavigateToDashboard`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onIntent(OnboardingUiIntent.PageChanged(1))

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.SkipClicked)

        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(OnboardingUiEffect.NavigateToDashboard)
    }

    // ── FinishClicked ────────────────────────────────────────────────────────

    @Test
    fun `FinishClicked calls markWelcomeSeen`() = runTest {
        val repo = FakeOnboardingStateRepository()
        val viewModel = buildViewModel(repo)

        backgroundScope.async { viewModel.effect.first() }.also {
            viewModel.onIntent(OnboardingUiIntent.FinishClicked)
            it.await()
        }

        assertThat(repo.welcomeSeenCalled).isTrue()
    }

    @Test
    fun `FinishClicked emits NavigateToDashboard`() = runTest {
        val viewModel = buildViewModel()

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(OnboardingUiIntent.FinishClicked)

        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(OnboardingUiEffect.NavigateToDashboard)
    }

    // ── isLastPage derivation ────────────────────────────────────────────────

    @Test
    fun `isLastPage is false on page 0 with 3-page carousel`() = runTest {
        val viewModel = buildViewModel()

        assertThat(viewModel.state.value.isLastPage).isFalse()
    }

    @Test
    fun `isLastPage is true only on page 2 with 3-page carousel`() = runTest {
        val viewModel = buildViewModel()

        repeat(OnboardingPage.all.size) { index ->
            viewModel.onIntent(OnboardingUiIntent.PageChanged(index))
            val expected = index == OnboardingPage.all.lastIndex
            assertThat(viewModel.state.value.isLastPage).isEqualTo(expected)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun buildViewModel(
        repo: OnboardingStateRepository = FakeOnboardingStateRepository(),
    ) = OnboardingViewModel(onboardingState = repo)
}

// ── Fake repository ──────────────────────────────────────────────────────────

private class FakeOnboardingStateRepository : OnboardingStateRepository {
    var welcomeSeen: Boolean = false
    var gradeHintSeen: Boolean = false
    var welcomeSeenCalled: Boolean = false
    var gradeHintSeenCalled: Boolean = false

    override fun hasSeenWelcome(): Boolean = welcomeSeen

    override fun markWelcomeSeen() {
        welcomeSeenCalled = true
        welcomeSeen = true
    }

    override fun hasSeenGradeHint(): Boolean = gradeHintSeen

    override fun markGradeHintSeen() {
        gradeHintSeenCalled = true
        gradeHintSeen = true
    }
}

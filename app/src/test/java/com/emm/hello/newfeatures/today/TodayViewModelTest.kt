package com.emm.hello.newfeatures.today

import app.cash.turbine.test
import com.emm.domain.study.DashboardStats
import com.emm.domain.study.GetDashboardStatsUseCase
import com.emm.domain.study.StudyStatsRepository
import com.emm.domain.time.Clock
import com.emm.hello.MainDispatcherRule
import com.emm.hello.newfeatures.study.StudyRoute
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone: ZoneId = ZoneId.systemDefault()

    @Test
    fun `initial state is loading and carries no stats`() = runTest {
        val viewModel = makeViewModel()

        assertThat(viewModel.state.value.isLoading).isTrue()
        assertThat(viewModel.state.value.stats).isNull()
    }

    @Test
    fun `ScreenVisible fetches stats and clears the loading flag`() = runTest {
        val fixedNow: Instant = Instant.parse("2026-05-04T12:00:00Z")
        val reviewDates: List<Long> = (0..6).map { dayOffset ->
            LocalDate.of(2026, 5, 4).minusDays(dayOffset.toLong())
                .atStartOfDay(zone).toInstant().toEpochMilli()
        }
        val statsUseCase = GetDashboardStatsUseCase(
            FakeStatsRepo(
                cardsStudiedToday = 5,
                cardsDueToday = 3,
                cardsDueThisWeek = 12,
                reviewDates = reviewDates,
            ),
            Clock { fixedNow },
        )
        val viewModel = makeViewModel(statsUseCase = statsUseCase)

        viewModel.onIntent(ScreenVisible)
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.stats?.cardsStudiedToday).isEqualTo(5)
        assertThat(viewModel.state.value.stats?.cardsDueToday).isEqualTo(3)
        assertThat(viewModel.state.value.stats?.currentStreak).isEqualTo(7)
        assertThat(viewModel.state.value.stats?.cardsDueThisWeek).isEqualTo(12)
    }

    @Test
    fun `StudyClicked always targets the all-due-decks session`() = runTest {
        val viewModel = makeViewModel()

        viewModel.effect.test {
            viewModel.onIntent(StudyClicked)
            val effect: TodayUiEffect = awaitItem()
            assertThat(effect).isInstanceOf(NavigateToStudy::class.java)
            assertThat((effect as NavigateToStudy).deckId).isEqualTo(StudyRoute.ALL_DUE_DECKS)
        }
    }

    @Test
    fun `no effect is emitted before an intent arrives`() = runTest {
        val viewModel = makeViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            expectNoEvents()
        }
    }

    @Test
    fun `session context stays empty while nothing is due`() = runTest {
        val viewModel = makeViewModel()

        viewModel.onIntent(ScreenVisible)
        advanceUntilIdle()

        assertThat(viewModel.state.value.hasSessionReady).isFalse()
        assertThat(viewModel.state.value.cardsDueToday).isEqualTo(0)
        assertThat(viewModel.state.value.estimatedSessionMinutes).isEqualTo(0)
    }

    @Test
    fun `estimated session minutes round up and never fall below one`() {
        assertThat(stateDueing(0).estimatedSessionMinutes).isEqualTo(0)
        assertThat(stateDueing(1).estimatedSessionMinutes).isEqualTo(1)
        assertThat(stateDueing(4).estimatedSessionMinutes).isEqualTo(1)
        assertThat(stateDueing(5).estimatedSessionMinutes).isEqualTo(2)
        assertThat(stateDueing(8).estimatedSessionMinutes).isEqualTo(2)
        assertThat(stateDueing(23).estimatedSessionMinutes).isEqualTo(6)

        assertThat(stateDueing(0).hasSessionReady).isFalse()
        assertThat(stateDueing(1).hasSessionReady).isTrue()
    }

    private fun stateDueing(cardsDueToday: Int): TodayUiState = TodayUiState(
        stats = DashboardStats(
            cardsStudiedToday = 0,
            cardsDueToday = cardsDueToday,
            currentStreak = 0,
            cardsDueThisWeek = cardsDueToday,
        ),
    )

    private fun makeViewModel(
        statsUseCase: GetDashboardStatsUseCase = makeDefaultStatsUseCase(),
    ): TodayViewModel = TodayViewModel(getDashboardStatsUseCase = statsUseCase)

    private fun makeDefaultStatsUseCase(): GetDashboardStatsUseCase = GetDashboardStatsUseCase(
        FakeStatsRepo(0, 0, 0, emptyList()),
        Clock { Instant.parse("2026-05-04T12:00:00Z") },
    )

    private class FakeStatsRepo(
        private val cardsStudiedToday: Int,
        private val cardsDueToday: Int,
        private val cardsDueThisWeek: Int,
        private val reviewDates: List<Long>,
        private val cardsDueInRange: Int = 0,
        private val nextReviewAt: Long? = null,
    ) : StudyStatsRepository {
        override suspend fun countDistinctCardsStudiedToday(): Int = cardsStudiedToday
        override suspend fun countCardsDueToday(): Int = cardsDueToday
        override suspend fun countCardsDueThisWeek(): Int = cardsDueThisWeek
        override suspend fun countCardsDueInRange(startMillis: Long, endMillis: Long): Int = cardsDueInRange
        override suspend fun findNextReviewAtAfter(millis: Long): Long? = nextReviewAt
        override suspend fun findReviewTimestampsDescending(): List<Long> = reviewDates
    }
}

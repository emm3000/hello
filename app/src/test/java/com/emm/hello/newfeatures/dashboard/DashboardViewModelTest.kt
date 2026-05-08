package com.emm.hello.newfeatures.dashboard

import app.cash.turbine.test
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toDeckId
import com.emm.domain.study.GetDashboardStatsUseCase
import com.emm.domain.study.StudyStatsRepository
import com.emm.domain.time.Clock
import com.emm.hello.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.systemDefault()

    @Test
    fun `initial state has isLoading true`() = runTest {
        val viewModel = makeViewModel(deckRepo = FakeDeckRepo(emitImmediately = false))

        assertThat(viewModel.uiState.value.isLoading).isTrue()
    }

    @Test
    fun `decks from repository appear in state and isLoading becomes false`() = runTest {
        val deck = Deck(
            id = "deck-1".toDeckId(),
            name = "Spanish",
            description = "",
            createdAt = LocalDateTime.parse("2026-03-18T10:00:00"),
            cards = emptyList(),
            cardsCount = 5L,
        )
        val viewModel = makeViewModel(deckRepo = FakeDeckRepo(decks = listOf(deck)))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.decks).containsExactly(deck)
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `view model emits no effects during deck loading`() = runTest {
        val viewModel = makeViewModel(deckRepo = FakeDeckRepo(decks = emptyList()))
        advanceUntilIdle()

        viewModel.effect.test {
            expectNoEvents()
        }
    }

    @Test
    fun `onVisible fetches stats and updates state`() = runTest {
        val fixedNow = Instant.parse("2026-05-04T12:00:00Z")
        val clock = Clock { fixedNow }

        // Build review dates that produce a streak of 7 (May 4 back to Apr 28)
        val reviewDates = (0..6).map { dayOffset ->
            LocalDate.of(2026, 5, 4).minusDays(dayOffset.toLong())
                .atStartOfDay(zone).toInstant().toEpochMilli()
        }

        val fakeStatsRepo = FakeStatsRepo(
            cardsStudiedToday = 5,
            cardsDueToday = 3,
            cardsDueThisWeek = 12,
            reviewDates = reviewDates,
        )
        val useCase = GetDashboardStatsUseCase(fakeStatsRepo, clock)

        val viewModel = makeViewModel(
            deckRepo = FakeDeckRepo(decks = emptyList()),
            statsUseCase = useCase,
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.stats).isNull()

        viewModel.onVisible()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.stats).isNotNull()
        assertThat(viewModel.uiState.value.stats?.cardsStudiedToday).isEqualTo(5)
        assertThat(viewModel.uiState.value.stats?.cardsDueToday).isEqualTo(3)
        assertThat(viewModel.uiState.value.stats?.currentStreak).isEqualTo(7)
        assertThat(viewModel.uiState.value.stats?.cardsDueThisWeek).isEqualTo(12)
    }

    private fun makeViewModel(
        deckRepo: FakeDeckRepo = FakeDeckRepo(),
        statsUseCase: GetDashboardStatsUseCase = makeDefaultStatsUseCase(),
    ): DashboardViewModel = DashboardViewModel(
        getDecksUseCase = GetDecksUseCase(deckRepo),
        getDashboardStatsUseCase = statsUseCase,
    )

    private fun makeDefaultStatsUseCase(): GetDashboardStatsUseCase {
        val repo = FakeStatsRepo(0, 0, 0, emptyList())
        val clock = Clock { Instant.parse("2026-05-04T12:00:00Z") }
        return GetDashboardStatsUseCase(repo, clock)
    }

    private class FakeDeckRepo(
        private val decks: List<Deck> = emptyList(),
        private val emitImmediately: Boolean = true,
    ) : DeckRepository {
        override suspend fun addDeck(deck: CreateDeckInput) = Unit
        override fun findById(deckId: DeckId): Flow<Deck> = emptyFlow()
        override fun fetchAll(): Flow<List<Deck>> = emptyFlow()
        override fun deckWithFlashcardCount(): Flow<List<Deck>> =
            if (emitImmediately) flowOf(decks) else emptyFlow()
    }

    private class FakeStatsRepo(
        private val cardsStudiedToday: Int,
        private val cardsDueToday: Int,
        private val cardsDueThisWeek: Int,
        private val reviewDates: List<Long>,
    ) : StudyStatsRepository {
        override suspend fun countDistinctCardsStudiedToday(): Int = cardsStudiedToday
        override suspend fun countCardsDueToday(): Int = cardsDueToday
        override suspend fun countCardsDueThisWeek(): Int = cardsDueThisWeek
        override suspend fun findDistinctReviewDatesDescending(): List<Long> = reviewDates
    }
}

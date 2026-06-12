package com.emm.hello.newfeatures.dashboard

import app.cash.turbine.test
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.DeckSearchCriteria
import com.emm.domain.deck.Tag
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.GetFilteredDecksUseCase
import com.emm.domain.deck.UpdateDeckInput
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toDeckId
import com.emm.domain.study.GetDashboardStatsUseCase
import com.emm.domain.study.StudyStatsRepository
import com.emm.domain.time.Clock
import com.emm.hello.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
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

        assertThat(viewModel.state.value.isLoading).isTrue()
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

        assertThat(viewModel.state.value.decks).containsExactly(deck)
        assertThat(viewModel.state.value.totalDeckCount).isEqualTo(1)
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun `query and tag intents update state and toggle filtering`() = runTest {
        val deck = sampleDeck(name = "Biology Exam", tags = listOf("exam", "biology"))
        val viewModel = makeViewModel(deckRepo = FakeDeckRepo(decks = listOf(deck)))
        advanceUntilIdle()

        viewModel.onIntent(QueryChanged("bio"))
        viewModel.onIntent(TagToggled("exam"))
        advanceUntilIdle()

        assertThat(viewModel.state.value.searchQuery).isEqualTo("bio")
        assertThat(viewModel.state.value.selectedTags).containsExactly("exam")
        assertThat(viewModel.state.value.isFiltering).isTrue()
    }

    @Test
    fun `clear filters resets criteria and filtered list`() = runTest {
        val deckA = sampleDeck(name = "Biology Exam", tags = listOf("exam", "biology"))
        val deckB = sampleDeck(id = "2", name = "English", tags = listOf("language"))
        val viewModel = makeViewModel(deckRepo = FakeDeckRepo(decks = listOf(deckA, deckB)))
        advanceUntilIdle()

        viewModel.onIntent(QueryChanged("bio"))
        advanceUntilIdle()
        assertThat(viewModel.state.value.decks).containsExactly(deckA)

        viewModel.onIntent(ClearFilters)
        advanceUntilIdle()

        assertThat(viewModel.state.value.searchQuery).isEmpty()
        assertThat(viewModel.state.value.selectedTags).isEmpty()
        assertThat(viewModel.state.value.isFiltering).isFalse()
        assertThat(viewModel.state.value.decks).containsExactly(deckA, deckB)
    }

    @Test
    fun `no results sets empty state to no results`() = runTest {
        val deck = sampleDeck(name = "Biology", tags = listOf("exam"))
        val viewModel = makeViewModel(deckRepo = FakeDeckRepo(decks = listOf(deck)))
        advanceUntilIdle()

        viewModel.onIntent(QueryChanged("history"))
        advanceUntilIdle()

        assertThat(viewModel.state.value.decks).isEmpty()
        assertThat(viewModel.state.value.emptyState).isEqualTo(DashboardEmptyState.NoResults)
    }

    @Test
    fun `empty library sets empty state to library empty`() = runTest {
        val viewModel = makeViewModel(deckRepo = FakeDeckRepo(decks = emptyList()))
        advanceUntilIdle()

        assertThat(viewModel.state.value.totalDeckCount).isEqualTo(0)
        assertThat(viewModel.state.value.decks).isEmpty()
        assertThat(viewModel.state.value.emptyState).isEqualTo(DashboardEmptyState.LibraryEmpty)
    }

    @Test
    fun `rendered list follows active criteria when source updates`() = runTest {
        val biologyExam = sampleDeck(id = "1", name = "Biology Exam", tags = listOf("exam", "biology"))
        val biologyPractice = sampleDeck(id = "2", name = "Biology Practice", tags = listOf("biology"))
        val englishExam = sampleDeck(id = "3", name = "English Exam", tags = listOf("exam"))
        val deckRepo = FakeDeckRepo(decks = listOf(biologyExam, biologyPractice, englishExam))
        val viewModel = makeViewModel(deckRepo = deckRepo)
        advanceUntilIdle()

        viewModel.onIntent(QueryChanged("bio"))
        viewModel.onIntent(TagToggled("exam"))
        advanceUntilIdle()

        assertThat(viewModel.state.value.decks).containsExactly(biologyExam)

        val bioNoExam = sampleDeck(id = "4", name = "Biology Notes", tags = listOf("biology"))
        val historyExam = sampleDeck(id = "5", name = "History Exam", tags = listOf("exam"))
        deckRepo.updateDecks(listOf(bioNoExam, historyExam))
        advanceUntilIdle()

        assertThat(viewModel.state.value.searchQuery).isEqualTo("bio")
        assertThat(viewModel.state.value.selectedTags).containsExactly("exam")
        assertThat(viewModel.state.value.decks).isEmpty()
        assertThat(viewModel.state.value.emptyState).isEqualTo(DashboardEmptyState.NoResults)
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
    fun `StudyClicked with decks emits NavigateToStudy with first deck id`() = runTest {
        val deck = sampleDeck(id = "deck-42", name = "English", tags = emptyList())
        val viewModel = makeViewModel(deckRepo = FakeDeckRepo(decks = listOf(deck)))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(StudyClicked)
            val effect = awaitItem()
            assertThat(effect).isInstanceOf(NavigateToStudy::class.java)
            assertThat((effect as NavigateToStudy).deckId).isEqualTo("deck-42")
        }
    }

    @Test
    fun `StudyClicked with no decks emits no effect`() = runTest {
        val viewModel = makeViewModel(deckRepo = FakeDeckRepo(decks = emptyList()))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(StudyClicked)
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

        assertThat(viewModel.state.value.stats).isNull()

        viewModel.onVisible()
        advanceUntilIdle()

        assertThat(viewModel.state.value.stats).isNotNull()
        assertThat(viewModel.state.value.stats?.cardsStudiedToday).isEqualTo(5)
        assertThat(viewModel.state.value.stats?.cardsDueToday).isEqualTo(3)
        assertThat(viewModel.state.value.stats?.currentStreak).isEqualTo(7)
        assertThat(viewModel.state.value.stats?.cardsDueThisWeek).isEqualTo(12)
    }

    private fun makeViewModel(
        deckRepo: FakeDeckRepo = FakeDeckRepo(),
        statsUseCase: GetDashboardStatsUseCase = makeDefaultStatsUseCase(),
    ): DashboardViewModel = DashboardViewModel(
        getDecksUseCase = GetDecksUseCase(deckRepo),
        getFilteredDecksUseCase = GetFilteredDecksUseCase(deckRepo),
        getDashboardStatsUseCase = statsUseCase,
    )

    private fun sampleDeck(
        id: String = "deck-1",
        name: String,
        tags: List<String>,
    ): Deck = Deck(
        id = id.toDeckId(),
        name = name,
        description = "",
        createdAt = LocalDateTime.parse("2026-03-18T10:00:00"),
        cards = emptyList(),
        cardsCount = 5L,
        tags = tags.map(::Tag),
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
        private val deckFlow = MutableStateFlow(decks)

        fun updateDecks(updatedDecks: List<Deck>) {
            deckFlow.value = updatedDecks
        }

        override suspend fun create(deck: CreateDeckInput) = Unit
        override fun fetchById(deckId: DeckId): Flow<Deck> = emptyFlow()
        override fun fetchAll(): Flow<List<Deck>> = emptyFlow()
        override fun deckWithFlashcardCount(): Flow<List<Deck>> =
            if (emitImmediately) deckFlow else emptyFlow()

        override fun observeFiltered(criteria: DeckSearchCriteria): Flow<List<Deck>> =
            deckWithFlashcardCount().map { source ->
                source.filter { deck ->
                    val queryMatches = criteria.normalizedQuery.isEmpty() ||
                        deck.name.lowercase().contains(criteria.normalizedQuery)
                    val tagsMatch = criteria.normalizedSelectedTags.all { selected ->
                        deck.tags.any { it.value.equals(selected, ignoreCase = true) }
                    }
                    queryMatches && tagsMatch
                }
            }

        override fun fetchTagsForDeck(deckId: DeckId): Flow<List<Tag>> = emptyFlow()

        override suspend fun update(input: UpdateDeckInput) = Unit
        override suspend fun softDeleteDeck(deckId: DeckId) = Unit
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

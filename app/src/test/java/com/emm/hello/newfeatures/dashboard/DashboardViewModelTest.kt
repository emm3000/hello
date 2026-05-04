package com.emm.hello.newfeatures.dashboard

import app.cash.turbine.test
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.GetDecksUseCase
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state has isLoading true`() = runTest {
        val viewModel = makeViewModel(deckRepo = FakeDeckRepo(emitImmediately = false))

        assertThat(viewModel.uiState.value.isLoading).isTrue()
    }

    @Test
    fun `decks from repository appear in state and isLoading becomes false`() = runTest {
        val deck = Deck(
            id = "deck-1",
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

    private fun makeViewModel(
        deckRepo: FakeDeckRepo = FakeDeckRepo(),
    ): DashboardViewModel = DashboardViewModel(
        getDecksUseCase = GetDecksUseCase(deckRepo),
    )

    private class FakeDeckRepo(
        private val decks: List<Deck> = emptyList(),
        private val emitImmediately: Boolean = true,
    ) : DeckRepository {
        override suspend fun addDeck(deck: CreateDeckInput) = Unit
        override fun findById(deckId: String): Flow<Deck> = emptyFlow()
        override fun fetchAll(): Flow<List<Deck>> = emptyFlow()
        override fun deckWithFlashcardCount(): Flow<List<Deck>> =
            if (emitImmediately) flowOf(decks) else emptyFlow()
    }
}

package com.emm.hello.newfeatures.dashboard

import app.cash.turbine.test
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.sync.GetSyncDebugStateUseCase
import com.emm.domain.sync.SyncDebugState
import com.emm.domain.sync.SyncDebugStateRepository
import com.emm.domain.sync.SyncEngine
import com.emm.domain.sync.SyncState
import com.emm.hello.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun `sync debug state is reflected in syncDebug field`() = runTest {
        val syncDebug = SyncDebugState(pendingOperations = 5L, deviceId = "device-abc")
        val viewModel = makeViewModel(syncDebugRepo = FakeSyncDebugRepo(syncDebug))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.syncDebug.pendingOperations).isEqualTo(5L)
        assertThat(viewModel.uiState.value.syncDebug.deviceId).isEqualTo("device-abc")
    }

    @Test
    fun `refresh sync failure emits sync failed effect with message`() = runTest {
        val viewModel = makeViewModel(syncEngine = FakeSyncEngine(shouldFail = true))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(DashboardUiIntent.RefreshSync)
            val effect = awaitItem()
            assertThat(effect).isInstanceOf(DashboardUiEffect.SyncFailed::class.java)
            assertThat((effect as DashboardUiEffect.SyncFailed).message).isEqualTo("sync error")
        }
    }

    @Test
    fun `refresh sync success does not emit any effect`() = runTest {
        val viewModel = makeViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(DashboardUiIntent.RefreshSync)
            expectNoEvents()
        }
    }

    private fun makeViewModel(
        deckRepo: FakeDeckRepo = FakeDeckRepo(),
        syncDebugRepo: FakeSyncDebugRepo = FakeSyncDebugRepo(),
        syncEngine: FakeSyncEngine = FakeSyncEngine(),
    ): DashboardViewModel = DashboardViewModel(
        getDecksUseCase = GetDecksUseCase(deckRepo),
        getSyncDebugStateUseCase = GetSyncDebugStateUseCase(syncDebugRepo),
        syncEngine = syncEngine,
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

    private class FakeSyncDebugRepo(
        private val state: SyncDebugState = SyncDebugState(),
    ) : SyncDebugStateRepository {
        override fun observe(): Flow<SyncDebugState> = flowOf(state)
    }

    private class FakeSyncEngine(private val shouldFail: Boolean = false) : SyncEngine {
        override val state = MutableStateFlow(SyncState())
        override suspend fun runOnce() {
            if (shouldFail) error("sync error")
        }
    }
}

package com.emm.hello.newfeatures.deck

import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.RestoreDeckUseCase
import com.emm.domain.deck.UpdateDeckInput
import com.emm.domain.ids.DeckId
import com.emm.hello.MainDispatcherRule
import com.emm.hello.R
import com.emm.hello.newfeatures.shared.UndoEvent
import com.emm.hello.newfeatures.shared.UndoEventHolder
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class DecksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is loading with no decks`() = runTest {
        val repository = FakeDeckRepository(decks = emptyFlow())
        val viewModel = DecksViewModel(
            restoreDeckUseCase = RestoreDeckUseCase(repository),
            undoEventHolder = UndoEventHolder(),
            getDecksUseCase = GetDecksUseCase(repository),
        )

        assertThat(viewModel.state.value.isLoading).isTrue()
        assertThat(viewModel.state.value.decks).isEmpty()
        assertThat(viewModel.state.value.isEmpty).isFalse()
    }

    @Test
    fun `decks from the use case replace state and clear loading`() = runTest {
        val deckOne: Deck = buildDeck(id = "deck-1", name = "Viajes")
        val deckTwo: Deck = buildDeck(id = "deck-2", name = "Comida")
        val repository = FakeDeckRepository(decks = flowOf(listOf(deckOne, deckTwo)))
        val viewModel = DecksViewModel(
            restoreDeckUseCase = RestoreDeckUseCase(repository),
            undoEventHolder = UndoEventHolder(),
            getDecksUseCase = GetDecksUseCase(repository),
        )

        assertThat(viewModel.state.value.decks).containsExactly(deckOne, deckTwo).inOrder()
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.isEmpty).isFalse()
    }

    @Test
    fun `empty deck list clears loading and marks state empty`() = runTest {
        val repository = FakeDeckRepository(decks = flowOf(emptyList()))
        val viewModel = DecksViewModel(
            restoreDeckUseCase = RestoreDeckUseCase(repository),
            undoEventHolder = UndoEventHolder(),
            getDecksUseCase = GetDecksUseCase(repository),
        )

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.isEmpty).isTrue()
    }

    @Test
    fun `deck opened emits open deck form with its id`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = DecksViewModel(
            restoreDeckUseCase = RestoreDeckUseCase(repository),
            undoEventHolder = UndoEventHolder(),
            getDecksUseCase = GetDecksUseCase(repository),
        )

        val effectDeferred: Deferred<List<DecksUiEffect>> =
            backgroundScope.async { viewModel.effect.take(1).toList() }
        viewModel.onIntent(DecksUiIntent.DeckOpened("deck-1"))

        val effects: List<DecksUiEffect> = effectDeferred.await()
        assertThat(effects).containsExactly(DecksUiEffect.OpenDeckForm("deck-1"))
    }

    @Test
    fun `create deck requested emits open deck form without id`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = DecksViewModel(
            restoreDeckUseCase = RestoreDeckUseCase(repository),
            undoEventHolder = UndoEventHolder(),
            getDecksUseCase = GetDecksUseCase(repository),
        )

        val effectDeferred: Deferred<List<DecksUiEffect>> =
            backgroundScope.async { viewModel.effect.take(1).toList() }
        viewModel.onIntent(DecksUiIntent.CreateDeckRequested)

        val effects: List<DecksUiEffect> = effectDeferred.await()
        assertThat(effects).containsExactly(DecksUiEffect.OpenDeckForm(null))
    }

    @Test
    fun `deck deleted undo event emits show undo effect`() = runTest {
        val repository = FakeDeckRepository()
        val undoEventHolder = UndoEventHolder()
        val viewModel = DecksViewModel(
            restoreDeckUseCase = RestoreDeckUseCase(repository),
            undoEventHolder = undoEventHolder,
            getDecksUseCase = GetDecksUseCase(repository),
        )

        val effectDeferred: Deferred<List<DecksUiEffect>> =
            backgroundScope.async { viewModel.effect.take(1).toList() }
        undoEventHolder.tryEmit(
            UndoEvent.DeckDeleted(deckId = "deck-1", deletedAt = DELETED_AT, deckName = "Viajes"),
        )

        val effects: List<DecksUiEffect> = effectDeferred.await()
        assertThat(effects).containsExactly(
            DecksUiEffect.ShowUndoDeckDeleted(deckName = "Viajes", deckId = "deck-1", deletedAt = DELETED_AT),
        )
    }

    @Test
    fun `undo delete deck restores through the use case`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = DecksViewModel(
            restoreDeckUseCase = RestoreDeckUseCase(repository),
            undoEventHolder = UndoEventHolder(),
            getDecksUseCase = GetDecksUseCase(repository),
        )
        val collector: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler)
        val effects: MutableList<DecksUiEffect> = mutableListOf()
        backgroundScope.launch(collector) { viewModel.effect.toList(effects) }

        viewModel.onIntent(DecksUiIntent.UndoDeleteDeck("deck-1", 42L))
        advanceUntilIdle()

        assertThat(repository.restoredDeckId).isEqualTo(DeckId.from("deck-1"))
        assertThat(repository.restoredDeletedAt).isEqualTo(42L)
        assertThat(effects).isEmpty()
    }

    @Test
    fun `undo delete deck failure emits restore error message`() = runTest {
        val repository = FakeDeckRepository(shouldFailRestore = true)
        val viewModel = DecksViewModel(
            restoreDeckUseCase = RestoreDeckUseCase(repository),
            undoEventHolder = UndoEventHolder(),
            getDecksUseCase = GetDecksUseCase(repository),
        )

        val effectDeferred: Deferred<DecksUiEffect> = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(DecksUiIntent.UndoDeleteDeck("deck-1", 42L))

        val effect: DecksUiEffect = effectDeferred.await()
        assertThat(effect).isEqualTo(DecksUiEffect.ShowMessage(R.string.error_restore_deck))
    }

    private fun buildDeck(id: String, name: String): Deck = Deck(
        id = DeckId.from(id),
        name = name,
        description = "",
        createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
        cards = emptyList(),
        cardsCount = 0L,
    )

    private class FakeDeckRepository(
        private val decks: Flow<List<Deck>> = emptyFlow(),
        private val shouldFailRestore: Boolean = false,
    ) : DeckRepository {

        var restoredDeckId: DeckId? = null
        var restoredDeletedAt: Long? = null

        override suspend fun create(deck: CreateDeckInput) = Unit

        override suspend fun update(input: UpdateDeckInput) = Unit

        override suspend fun softDeleteDeck(deckId: DeckId): Long = DELETED_AT

        override suspend fun restoreDeck(deckId: DeckId, deletedAt: Long) {
            if (shouldFailRestore) error("boom")
            restoredDeckId = deckId
            restoredDeletedAt = deletedAt
        }

        override fun fetchById(deckId: DeckId): Flow<Deck?> = emptyFlow()

        override fun fetchAll(): Flow<List<Deck>> = emptyFlow()

        override fun deckWithFlashcardCount(): Flow<List<Deck>> = decks
    }
}

private const val DELETED_AT: Long = 1_700_000_000_000L

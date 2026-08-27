package com.emm.hello.newfeatures.deck

import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.SoftDeleteDeckUseCase
import com.emm.domain.deck.UpdateDeckInput
import com.emm.domain.deck.UpdateDeckUseCase
import com.emm.domain.ids.DeckId
import com.emm.hello.MainDispatcherRule
import com.emm.hello.R
import com.emm.hello.newfeatures.shared.UndoEvent
import com.emm.hello.newfeatures.shared.UndoEventHolder
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class NewDeckViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `submit success resets state and emits success message then navigate back effect`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = NewDeckViewModel(
            deckRepository = repository,
            updateDeckUseCase = UpdateDeckUseCase(repository),
            softDeleteDeckUseCase = SoftDeleteDeckUseCase(repository),
            undoEventHolder = UndoEventHolder(),
            formMode = DeckFormMode.Create,
        )

        viewModel.onIntent(NewDeckUiIntent.NameChanged("My deck"))
        viewModel.onIntent(NewDeckUiIntent.DescriptionChanged("Optional"))

        val effectsDeferred = backgroundScope.async { viewModel.effect.take(2).toList() }
        viewModel.onIntent(NewDeckUiIntent.Submit)

        val effects = effectsDeferred.await()
        assertThat(effects).containsExactly(
            NewDeckUiEffect.ShowMessage(R.string.deck_created_message),
            NewDeckUiEffect.NavigateBack,
        ).inOrder()
        assertThat(viewModel.state.value.name).isEmpty()
        assertThat(viewModel.state.value.description).isEmpty()
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(repository.lastAdded).isEqualTo(CreateDeckInput("My deck", "Optional", emptyList()))
    }

    @Test
    fun `submit with tags passes normalized tags to repository`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = NewDeckViewModel(
            deckRepository = repository,
            updateDeckUseCase = UpdateDeckUseCase(repository),
            softDeleteDeckUseCase = SoftDeleteDeckUseCase(repository),
            undoEventHolder = UndoEventHolder(),
            formMode = DeckFormMode.Create,
        )

        viewModel.onIntent(NewDeckUiIntent.NameChanged("Travel Deck"))
        viewModel.onIntent(NewDeckUiIntent.TagsChanged(listOf("  Spanish ", "TRAVEL", "spanish")))

        val effectsDeferred = backgroundScope.async { viewModel.effect.take(2).toList() }
        viewModel.onIntent(NewDeckUiIntent.Submit)

        val effects = effectsDeferred.await()
        assertThat(effects).containsExactly(
            NewDeckUiEffect.ShowMessage(R.string.deck_created_message),
            NewDeckUiEffect.NavigateBack,
        ).inOrder()
        // Tags should be normalized: lowercase, trimmed, deduplicated
        assertThat(repository.lastAdded?.tags).isEqualTo(listOf("spanish", "travel"))
    }

    @Test
    fun `tags changed normalizes tags before updating state`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = NewDeckViewModel(
            deckRepository = repository,
            updateDeckUseCase = UpdateDeckUseCase(repository),
            softDeleteDeckUseCase = SoftDeleteDeckUseCase(repository),
            undoEventHolder = UndoEventHolder(),
            formMode = DeckFormMode.Create,
        )

        viewModel.onIntent(NewDeckUiIntent.TagsChanged(listOf("  Mixed ", "UPPERCASE", "mixed", "")))

        assertThat(viewModel.state.value.tags).isEqualTo(listOf("mixed", "uppercase"))
    }

    @Test
    fun `tags changed removes blank tags`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = NewDeckViewModel(
            deckRepository = repository,
            updateDeckUseCase = UpdateDeckUseCase(repository),
            softDeleteDeckUseCase = SoftDeleteDeckUseCase(repository),
            undoEventHolder = UndoEventHolder(),
            formMode = DeckFormMode.Create,
        )

        viewModel.onIntent(NewDeckUiIntent.TagsChanged(listOf("valid", "   ", "", "  ")))

        assertThat(viewModel.state.value.tags).isEqualTo(listOf("valid"))
    }

    @Test
    fun `submit failure emits show message and stops loading`() = runTest {
        val repository = FakeDeckRepository(shouldFail = true)
        val viewModel = NewDeckViewModel(
            deckRepository = repository,
            updateDeckUseCase = UpdateDeckUseCase(repository),
            softDeleteDeckUseCase = SoftDeleteDeckUseCase(repository),
            undoEventHolder = UndoEventHolder(),
            formMode = DeckFormMode.Create,
        )

        viewModel.onIntent(NewDeckUiIntent.NameChanged("Deck with error"))

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(NewDeckUiIntent.Submit)

        val effect = effectDeferred.await()
        assertThat(effect).isInstanceOf(NewDeckUiEffect.ShowMessage::class.java)
        assertThat((effect as NewDeckUiEffect.ShowMessage).messageRes).isEqualTo(R.string.error_create_deck)
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(repository.createCalls).isEqualTo(1)
    }

    @Test
    fun `confirming delete in edit mode soft deletes the deck and publishes an undo event`() = runTest {
        val repository = FakeDeckRepository()
        repository.storedDeck = Deck(
            id = DeckId.from("deck-1"),
            name = "Viajes",
            description = "",
            createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
            cards = emptyList(),
            cardsCount = 0L,
        )
        val undoEventHolder = UndoEventHolder()
        val viewModel = NewDeckViewModel(
            deckRepository = repository,
            updateDeckUseCase = UpdateDeckUseCase(repository),
            softDeleteDeckUseCase = SoftDeleteDeckUseCase(repository),
            undoEventHolder = undoEventHolder,
            formMode = DeckFormMode.Edit("deck-1"),
        )

        val collector: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler)
        val undoEvents = mutableListOf<UndoEvent>()
        backgroundScope.launch(collector) { undoEventHolder.events.toList(undoEvents) }
        val effects = mutableListOf<NewDeckUiEffect>()
        backgroundScope.launch(collector) { viewModel.effect.toList(effects) }

        viewModel.onIntent(NewDeckUiIntent.DeleteDeck)
        assertThat(viewModel.state.value.isDeleteConfirmationVisible).isTrue()

        viewModel.onIntent(NewDeckUiIntent.ConfirmDeleteDeck)
        advanceUntilIdle()

        assertThat(effects).contains(NewDeckUiEffect.DeckDeleted)
        assertThat(undoEvents).containsExactly(
            UndoEvent.DeckDeleted(deckId = "deck-1", deletedAt = DELETED_AT, deckName = "Viajes"),
        )
        assertThat(repository.softDeletedDeckId).isEqualTo(DeckId.from("deck-1"))
        assertThat(viewModel.state.value.isDeleteConfirmationVisible).isFalse()
    }

    @Test
    fun `confirming delete in create mode never reaches the repository`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = NewDeckViewModel(
            deckRepository = repository,
            updateDeckUseCase = UpdateDeckUseCase(repository),
            softDeleteDeckUseCase = SoftDeleteDeckUseCase(repository),
            undoEventHolder = UndoEventHolder(),
            formMode = DeckFormMode.Create,
        )

        viewModel.onIntent(NewDeckUiIntent.ConfirmDeleteDeck)
        advanceUntilIdle()

        assertThat(repository.softDeletedDeckId).isNull()
        assertThat(viewModel.state.value.canDelete).isFalse()
    }

    private class FakeDeckRepository(
        private val shouldFail: Boolean = false,
    ) : DeckRepository {
        var createCalls: Int = 0
        var lastAdded: CreateDeckInput? = null

        override suspend fun create(deck: CreateDeckInput) {
            createCalls += 1
            lastAdded = deck
            if (shouldFail) error("boom")
        }

        var storedDeck: Deck? = null

        override fun fetchById(deckId: DeckId): Flow<Deck> =
            storedDeck?.let { flowOf(it) } ?: emptyFlow()

        override fun fetchAll(): Flow<List<Deck>> = emptyFlow()

        override fun deckWithFlashcardCount(): Flow<List<Deck>> = emptyFlow()

        override suspend fun update(input: UpdateDeckInput) = Unit
        var softDeletedDeckId: DeckId? = null

        override suspend fun softDeleteDeck(deckId: DeckId): Long {
            softDeletedDeckId = deckId
            return DELETED_AT
        }
        override suspend fun restoreDeck(deckId: DeckId, deletedAt: Long) = Unit
    }
}

private const val DELETED_AT: Long = 1_700_000_000_000L

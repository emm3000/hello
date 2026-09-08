package com.emm.hello.newfeatures.store

import com.emm.domain.curated.CuratedDeck
import com.emm.domain.curated.CuratedDeckCatalog
import com.emm.domain.curated.GetCuratedDecksUseCase
import com.emm.domain.curated.InstallCuratedDeckUseCase
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.UpdateDeckInput
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.LevelBand
import com.emm.domain.ids.DeckId
import com.emm.hello.MainDispatcherRule
import com.emm.hello.R
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class StoreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `listings load into state with isLoading false`() = runTest {
        val installedDeck: Deck = buildDeck(DeckId.from("curated-$INSTALLED_ID"))
        val viewModel: StoreViewModel = buildViewModel(
            catalog = FakeCuratedDeckCatalog(
                listOf(
                    buildCuratedDeck(id = INSTALLED_ID, noteCount = 3),
                    buildCuratedDeck(id = CURATED_ID, noteCount = 5),
                ),
            ),
            deckRepository = FakeDeckRepository(decks = flowOf(listOf(installedDeck))),
        )

        val decks: List<StoreDeckItem> = viewModel.state.value.decks
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(decks).hasSize(2)
        assertThat(decks[0].installedDeckId).isEqualTo("curated-$INSTALLED_ID")
        assertThat(decks[0].isInstalled).isTrue()
        assertThat(decks[0].cardsCount).isEqualTo(3)
        assertThat(decks[1].installedDeckId).isNull()
        assertThat(decks[1].cardsCount).isEqualTo(5)
    }

    @Test
    fun `install marks the deck busy then clears it`() = runTest {
        val install: CompletableDeferred<Unit> = CompletableDeferred()
        val installCuratedDeckUseCase: InstallCuratedDeckUseCase = mockk()
        coEvery { installCuratedDeckUseCase(CURATED_ID) } coAnswers {
            install.await()
            DeckId.from("curated-$CURATED_ID")
        }
        val viewModel: StoreViewModel = buildViewModel(
            installCuratedDeckUseCase = installCuratedDeckUseCase,
        )

        viewModel.onIntent(StoreUiIntent.InstallRequested(CURATED_ID))
        assertThat(viewModel.state.value.installingDeckId).isEqualTo(CURATED_ID)

        install.complete(Unit)
        assertThat(viewModel.state.value.installingDeckId).isNull()
    }

    @Test
    fun `a failed install clears busy and shows a message`() = runTest {
        val installCuratedDeckUseCase: InstallCuratedDeckUseCase = mockk()
        coEvery { installCuratedDeckUseCase(CURATED_ID) } throws IllegalStateException("boom")
        val viewModel: StoreViewModel = buildViewModel(
            installCuratedDeckUseCase = installCuratedDeckUseCase,
        )

        val effectDeferred: Deferred<StoreUiEffect> = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(StoreUiIntent.InstallRequested(CURATED_ID))

        assertThat(effectDeferred.await()).isEqualTo(StoreUiEffect.ShowMessage(R.string.store_install_failed))
        assertThat(viewModel.state.value.installingDeckId).isNull()
    }

    @Test
    fun `open deck emits the deck id`() = runTest {
        val viewModel: StoreViewModel = buildViewModel()

        val effectDeferred: Deferred<StoreUiEffect> = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(StoreUiIntent.OpenDeckRequested("curated-$INSTALLED_ID"))

        assertThat(effectDeferred.await()).isEqualTo(StoreUiEffect.OpenDeck("curated-$INSTALLED_ID"))
    }

    @Test
    fun `back emits navigate back`() = runTest {
        val viewModel: StoreViewModel = buildViewModel()

        val effectDeferred: Deferred<StoreUiEffect> = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(StoreUiIntent.BackRequested)

        assertThat(effectDeferred.await()).isEqualTo(StoreUiEffect.NavigateBack)
    }

    private fun buildViewModel(
        catalog: CuratedDeckCatalog = FakeCuratedDeckCatalog(),
        deckRepository: DeckRepository = FakeDeckRepository(),
        installCuratedDeckUseCase: InstallCuratedDeckUseCase = mockk(),
    ): StoreViewModel = StoreViewModel(
        getCuratedDecksUseCase = GetCuratedDecksUseCase(catalog, deckRepository),
        installCuratedDeckUseCase = installCuratedDeckUseCase,
    )

    private fun buildCuratedDeck(id: String, noteCount: Int): CuratedDeck = CuratedDeck(
        id = id,
        name = "Deck $id",
        description = "Description $id",
        tags = listOf("false friends"),
        levelBand = LevelBand.B1_B2,
        notes = List(noteCount) { mockk<GeneratedLearningNote>() },
    )

    private fun buildDeck(deckId: DeckId): Deck = Deck(
        id = deckId,
        name = "Installed",
        description = "",
        createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
        cards = emptyList(),
        cardsCount = 0L,
    )

    private class FakeCuratedDeckCatalog(
        private val curatedDecks: List<CuratedDeck> = emptyList(),
    ) : CuratedDeckCatalog {

        override fun decks(): List<CuratedDeck> = curatedDecks
    }

    private class FakeDeckRepository(
        private val decks: Flow<List<Deck>> = emptyFlow(),
    ) : DeckRepository {

        override suspend fun create(deck: CreateDeckInput) = Unit

        override suspend fun update(input: UpdateDeckInput) = Unit

        override suspend fun softDeleteDeck(deckId: DeckId): Long = 0L

        override suspend fun restoreDeck(deckId: DeckId, deletedAt: Long) = Unit

        override fun fetchById(deckId: DeckId): Flow<Deck?> = emptyFlow()

        override fun fetchAll(): Flow<List<Deck>> = emptyFlow()

        override fun deckWithFlashcardCount(): Flow<List<Deck>> = decks
    }
}

private const val CURATED_ID: String = "spanish-traps"
private const val INSTALLED_ID: String = "phrasal-verbs"

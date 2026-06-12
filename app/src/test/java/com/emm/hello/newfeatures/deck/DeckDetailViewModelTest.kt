package com.emm.hello.newfeatures.deck

import app.cash.turbine.test
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.DeckSearchCriteria
import com.emm.domain.deck.GetDeckDetailUseCase
import com.emm.domain.deck.SoftDeleteDeckUseCase
import com.emm.domain.deck.Tag
import com.emm.domain.deck.UpdateDeckInput
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.RestoreFlashcardUseCase
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.study.ObserveFlashcardsWithReviewUseCase
import com.emm.domain.study.StudyFlashcard
import com.emm.domain.study.StudySessionRepository
import com.emm.domain.time.Clock
import com.emm.hello.MainDispatcherRule
import com.emm.hello.newfeatures.shared.UndoEvent
import com.emm.hello.newfeatures.shared.UndoEventHolder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

class DeckDetailViewModelTest {

    private val fixedClock = Clock { Instant.EPOCH }

    @Test
    fun `matchesSearchQuery returns true for empty query`() {
        val card = testCard(word = "hello", translation = "hola", meaning = "greeting")
        assertThat(matchesSearchQuery(card, "")).isTrue()
    }

    @Test
    fun `matchesSearchQuery returns true for whitespace-only query`() {
        val card = testCard(word = "hello", translation = "hola", meaning = "greeting")
        assertThat(matchesSearchQuery(card, "   ")).isTrue()
    }

    @Test
    fun `matchesSearchQuery matches word case-insensitive`() {
        val card = testCard(word = "Serendipity", translation = "hola", meaning = "greeting")
        assertThat(matchesSearchQuery(card, "serendipity")).isTrue()
        assertThat(matchesSearchQuery(card, "SERENDIPITY")).isTrue()
        assertThat(matchesSearchQuery(card, "rendip")).isTrue()
    }

    @Test
    fun `matchesSearchQuery matches translation case-insensitive`() {
        val card = testCard(word = "hello", translation = "Casualidad afortunada", meaning = "greeting")
        assertThat(matchesSearchQuery(card, "casualidad")).isTrue()
        assertThat(matchesSearchQuery(card, "Afortunada")).isTrue()
    }

    @Test
    fun `matchesSearchQuery matches meaning case-insensitive`() {
        val card = testCard(word = "hello", translation = "hola", meaning = "The occurrence of events by chance")
        assertThat(matchesSearchQuery(card, "occurrence")).isTrue()
        assertThat(matchesSearchQuery(card, "CHANCE")).isTrue()
    }

    @Test
    fun `matchesSearchQuery returns false when no field matches`() {
        val card = testCard(word = "hello", translation = "hola", meaning = "greeting")
        assertThat(matchesSearchQuery(card, "xyz")).isFalse()
    }

    @Test
    fun `matchesSearchQuery treats special characters as literal text`() {
        val card = testCard(word = "hello+world", translation = "hola", meaning = "greeting")
        assertThat(matchesSearchQuery(card, "+")).isTrue()
        assertThat(matchesSearchQuery(card, ".*")).isFalse()
    }

    @Test
    fun `matchesSearchQuery works with very long query without crashing`() {
        val card = testCard(word = "hello", translation = "hola", meaning = "greeting")
        val longQuery = "a".repeat(500)
        assertThat(matchesSearchQuery(card, longQuery)).isFalse()
    }

    @Test
    fun `matchesSearchQuery trims query before matching`() {
        val card = testCard(word = "hello", translation = "hola", meaning = "greeting")
        assertThat(matchesSearchQuery(card, "  hello  ")).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `onIntent SearchCardsChanged updates searchQuery in state`() = runTest {
        val viewModel = makeViewModel()
        advanceUntilIdle()

        viewModel.onIntent(DeckDetailUiIntent.SearchCardsChanged("hello"))

        assertThat(viewModel.state.value.searchQuery).isEqualTo("hello")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `onIntent SearchCardsChanged with empty string clears query`() = runTest {
        val viewModel = makeViewModel()
        advanceUntilIdle()

        viewModel.onIntent(DeckDetailUiIntent.SearchCardsChanged("hello"))
        assertThat(viewModel.state.value.searchQuery).isEqualTo("hello")

        viewModel.onIntent(DeckDetailUiIntent.SearchCardsChanged(""))
        assertThat(viewModel.state.value.searchQuery).isEqualTo("")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `initial state has empty searchQuery`() = runTest {
        val viewModel = makeViewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.searchQuery).isEmpty()
    }

    @Test
    fun `merge deck cards by id preserves all deck cards when session list is shorter`() {
        val baseReview = FlashcardReview.empty(fixedClock).copy(nextReviewAt = 10L)
        val updatedReview = FlashcardReview.empty(fixedClock).copy(nextReviewAt = 99L)
        val cardA = flashcard(id = "a", review = baseReview)
        val cardB = flashcard(id = "b", review = baseReview)

        val merged = mergeDeckCardsById(
            deckCards = listOf(cardA, cardB),
            sessionCards = listOf(flashcard(id = "a", review = updatedReview)),
        )

        assertThat(merged).hasSize(2)
        assertThat(merged[0].review.nextReviewAt).isEqualTo(99L)
        assertThat(merged[1].review).isSameInstanceAs(cardB.review)
    }

    private fun flashcard(id: String, review: FlashcardReview): Flashcard = Flashcard(
        id = id.toFlashcardId(),
        word = id,
        meaning = "",
        translation = "",
        examples = emptyList(),
        phonetic = "",
        review = review,
    )

    private fun testCard(
        word: String = "",
        translation: String = "",
        meaning: String = "",
    ): Flashcard = Flashcard(
        id = "test".toFlashcardId(),
        word = word,
        meaning = meaning,
        translation = translation,
        examples = emptyList(),
        phonetic = "",
        review = FlashcardReview.empty(fixedClock),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `deck deleted while observed emits no crash — state remains`() = runTest {
        val repo = FakeDeckRepoNullable()
        val realDeck = Deck(
            id = DeckId.from("deck-1"),
            name = "Spanish",
            description = "",
            createdAt = LocalDateTime.parse("2026-03-18T10:00:00"),
            cards = emptyList(),
            cardsCount = 0L,
        )
        val cardRepo = FakeCardRepo()
        val viewModel = DeckDetailViewModel(
            deckId = "deck-1",
            getDeckDetailUseCase = GetDeckDetailUseCase(repo, cardRepo),
            observeFlashcardsWithReviewUseCase = ObserveFlashcardsWithReviewUseCase(FakeStudyRepo()),
            softDeleteDeckUseCase = SoftDeleteDeckUseCase(repo),
            restoreFlashcardUseCase = RestoreFlashcardUseCase(cardRepo),
            undoEventHolder = UndoEventHolder(),
        )
        // Emit real deck so combine fires and state is populated
        repo.emitDeck(realDeck)
        advanceUntilIdle()
        assertThat(viewModel.state.value.deck.id.value).isEqualTo("deck-1")

        // Emit null to simulate post-deletion query returning nothing
        repo.emitNull()
        advanceUntilIdle()
        // filterNotNull dropped the null emission; state still holds the real deck
        assertThat(viewModel.state.value.deck.id.value).isEqualTo("deck-1")
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun makeViewModel(
        deckRepo: FakeDeckRepo = FakeDeckRepo(),
        cardRepo: FakeCardRepo = FakeCardRepo(),
        undoEventHolder: UndoEventHolder = UndoEventHolder(),
    ): DeckDetailViewModel {
        val studyRepo = FakeStudyRepo()
        return DeckDetailViewModel(
            deckId = "deck-1",
            getDeckDetailUseCase = GetDeckDetailUseCase(deckRepo, cardRepo),
            observeFlashcardsWithReviewUseCase = ObserveFlashcardsWithReviewUseCase(studyRepo),
            softDeleteDeckUseCase = SoftDeleteDeckUseCase(deckRepo),
            restoreFlashcardUseCase = RestoreFlashcardUseCase(cardRepo),
            undoEventHolder = undoEventHolder,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `deleteDeck emits DeckDeleted undo event to holder`() = runTest {
        val holder = UndoEventHolder()
        val deckRepo = FakeDeckRepo()
        val viewModel = makeViewModel(deckRepo = deckRepo, undoEventHolder = holder)
        advanceUntilIdle()

        holder.events.test {
            viewModel.onIntent(DeckDetailUiIntent.ConfirmDeleteDeck)
            advanceUntilIdle()
            val event = awaitItem()
            assertThat(event).isInstanceOf(UndoEvent.DeckDeleted::class.java)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `CardDeleted undo event from holder emits ShowUndoCardDeleted effect`() = runTest {
        val holder = UndoEventHolder()
        val viewModel = makeViewModel(undoEventHolder = holder)
        advanceUntilIdle()

        viewModel.effect.test {
            holder.tryEmit(UndoEvent.CardDeleted(flashcardId = "card-1", deletedAt = 888L))
            val effect = awaitItem()
            assertThat(effect).isInstanceOf(DeckDetailUiEffect.ShowUndoCardDeleted::class.java)
            val undoEffect = effect as DeckDetailUiEffect.ShowUndoCardDeleted
            assertThat(undoEffect.flashcardId).isEqualTo("card-1")
            assertThat(undoEffect.deletedAt).isEqualTo(888L)
        }
    }

    private class FakeDeckRepo : DeckRepository {
        override suspend fun create(deck: com.emm.domain.deck.CreateDeckInput) = Unit
        override fun fetchById(deckId: DeckId): Flow<Deck?> = emptyFlow()
        override fun fetchAll(): Flow<List<Deck>> = emptyFlow()
        override fun deckWithFlashcardCount(): Flow<List<Deck>> = emptyFlow()
        override fun observeFiltered(criteria: DeckSearchCriteria): Flow<List<Deck>> = flowOf(emptyList())
        override fun fetchTagsForDeck(deckId: DeckId): Flow<List<Tag>> = emptyFlow()
        override suspend fun update(input: UpdateDeckInput) = Unit
        override suspend fun softDeleteDeck(deckId: DeckId): Long = 0L
        override suspend fun restoreDeck(deckId: DeckId, deletedAt: Long) = Unit
    }

    private class FakeDeckRepoNullable : DeckRepository {
        private val flow = MutableSharedFlow<Deck?>(replay = 1)
        fun emitDeck(deck: Deck) { flow.tryEmit(deck) }
        fun emitNull() { flow.tryEmit(null) }
        override fun fetchById(deckId: DeckId): Flow<Deck?> = flow
        override suspend fun create(deck: com.emm.domain.deck.CreateDeckInput) = Unit
        override fun fetchAll(): Flow<List<Deck>> = emptyFlow()
        override fun deckWithFlashcardCount(): Flow<List<Deck>> = emptyFlow()
        override fun observeFiltered(criteria: DeckSearchCriteria): Flow<List<Deck>> = flowOf(emptyList())
        override fun fetchTagsForDeck(deckId: DeckId): Flow<List<Tag>> = emptyFlow()
        override suspend fun update(input: UpdateDeckInput) = Unit
        override suspend fun softDeleteDeck(deckId: DeckId): Long = 0L
        override suspend fun restoreDeck(deckId: DeckId, deletedAt: Long) = Unit
    }

    private class FakeCardRepo : FlashcardRepository {
        override fun fetchAll(): Flow<List<Flashcard>> = emptyFlow()
        override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> = flowOf(emptyList())
        override suspend fun fetchById(id: FlashcardId): FlashcardDetail =
            throw NotImplementedError()
        override suspend fun create(input: com.emm.domain.flashcard.CreateFlashcardInput): FlashcardId =
            throw NotImplementedError()
        override suspend fun upsertExamples(
            examples: List<com.emm.domain.flashcard.Example>,
            flashcardId: FlashcardId,
        ) = Unit
        override suspend fun update(input: UpdateFlashcardInput) = Unit
        override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = 0L
        override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = Unit
        override suspend fun countDueFlashcards(nowMillis: Long): Long = 0L
    }

    private class FakeStudyRepo(
        private val items: List<StudyFlashcard> = emptyList(),
    ) : StudySessionRepository {
        override fun flashcardWithReview(deckId: DeckId): Flow<List<StudyFlashcard>> =
            flowOf(items)
        override suspend fun sessionToday(deckId: DeckId): List<StudyFlashcard> = emptyList()
    }
}

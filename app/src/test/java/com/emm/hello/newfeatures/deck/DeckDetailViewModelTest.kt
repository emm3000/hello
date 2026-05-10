package com.emm.hello.newfeatures.deck

import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.DeckSearchCriteria
import com.emm.domain.deck.Tag
import com.emm.domain.deck.GetDeckDetailUseCase
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.FlashcardReview
import com.emm.hello.MainDispatcherRule
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.study.StudyFlashcard
import com.emm.domain.study.StudySessionRepository
import com.emm.domain.study.ObserveFlashcardsWithReviewUseCase
import com.emm.domain.time.Clock
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

class DeckDetailViewModelTest {

    private val fixedClock = Clock { Instant.EPOCH }

    // ── matchesSearchQuery tests ─────────────────────────────────────────

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

    // ── ViewModel search intent tests ────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `onIntent SearchCardsChanged updates searchQuery in state`() = runTest {
        val viewModel = makeViewModel()
        advanceUntilIdle()

        viewModel.onIntent(DeckDetailUiIntent.SearchCardsChanged("hello"))

        assertThat(viewModel.uiState.value.searchQuery).isEqualTo("hello")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `onIntent SearchCardsChanged with empty string clears query`() = runTest {
        val viewModel = makeViewModel()
        advanceUntilIdle()

        viewModel.onIntent(DeckDetailUiIntent.SearchCardsChanged("hello"))
        assertThat(viewModel.uiState.value.searchQuery).isEqualTo("hello")

        viewModel.onIntent(DeckDetailUiIntent.SearchCardsChanged(""))
        assertThat(viewModel.uiState.value.searchQuery).isEqualTo("")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `initial state has empty searchQuery`() = runTest {
        val viewModel = makeViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.searchQuery).isEmpty()
    }

    // ── mergeDeckCardsById tests ─────────────────────────────────────────

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

    // ── ViewModel test helpers ───────────────────────────────────────────

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun makeViewModel(): DeckDetailViewModel {
        val deckRepo = FakeDeckRepo()
        val cardRepo = FakeCardRepo()
        val studyRepo = FakeStudyRepo()
        return DeckDetailViewModel(
            deckId = "deck-1",
            getDeckDetailUseCase = GetDeckDetailUseCase(deckRepo, cardRepo),
            observeFlashcardsWithReviewUseCase = ObserveFlashcardsWithReviewUseCase(studyRepo),
        )
    }

    private class FakeDeckRepo : DeckRepository {
        override suspend fun addDeck(deck: com.emm.domain.deck.CreateDeckInput) = Unit
        override fun findById(deckId: DeckId): Flow<Deck> = emptyFlow()
        override fun fetchAll(): Flow<List<Deck>> = emptyFlow()
        override fun deckWithFlashcardCount(): Flow<List<Deck>> = emptyFlow()
        override fun observeFiltered(criteria: DeckSearchCriteria): Flow<List<Deck>> = flowOf(emptyList())
        override fun findTagsForDeck(deckId: DeckId): Flow<List<Tag>> = emptyFlow()
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
    }

    private class FakeStudyRepo : StudySessionRepository {
        override fun flashcardWithReview(deckId: DeckId): Flow<List<StudyFlashcard>> =
            flowOf(emptyList())
        override suspend fun sessionToday(deckId: DeckId): List<StudyFlashcard> = emptyList()
    }
}

package com.emm.hello.newfeatures.capture

import app.cash.turbine.test
import com.emm.domain.authoring.CaptureFlashcardUseCase
import com.emm.domain.authoring.RetryFailedEnrichmentsUseCase
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.flashcard.EnrichmentBacklog
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.FlashcardEnrichmentRepository
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.library.LibraryFlashcard
import com.emm.domain.library.LibraryRepository
import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import com.emm.hello.MainDispatcherRule
import com.emm.hello.R
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `submit enqueues enrichment for the captured card`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        coEvery { captureFlashcard(any(), any()) } returns CARD_ID
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))

        viewModel.effect.test {
            viewModel.onIntent(CaptureUiIntent.Submit)
            assertThat(awaitItem()).isEqualTo(CaptureUiEffect.EnqueueEnrichment(listOf("card-1")))
            assertThat(awaitItem()).isEqualTo(CaptureUiEffect.ShowMessage(R.string.capture_saved_message))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit captures into the default deck`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        coEvery { captureFlashcard(any(), any()) } returns CARD_ID
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))
        viewModel.onIntent(CaptureUiIntent.Submit)
        advanceUntilIdle()

        coVerify { captureFlashcard(deckId = DECK_ID, word = "borrow") }
    }

    @Test
    fun `submit clears the field`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        coEvery { captureFlashcard(any(), any()) } returns CARD_ID
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))
        viewModel.onIntent(CaptureUiIntent.Submit)
        advanceUntilIdle()

        assertThat(viewModel.state.value.word).isEmpty()
    }

    @Test
    fun `a duplicate word reports it and enqueues nothing`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        coEvery { captureFlashcard(any(), any()) } throws DomainValidationException(
            issues = listOf(ValidationIssue.Error(code = IssueCode.DuplicateWordInDeck, field = "word")),
        )
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))

        viewModel.effect.test {
            viewModel.onIntent(CaptureUiIntent.Submit)
            assertThat(awaitItem()).isEqualTo(CaptureUiEffect.ShowMessage(R.string.capture_error_duplicate))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry enqueues every card that had failed`() = runTest {
        val retryFailedEnrichments = mockk<RetryFailedEnrichmentsUseCase>()
        coEvery { retryFailedEnrichments() } returns listOf("card-a".toFlashcardId(), "card-b".toFlashcardId())
        val viewModel = buildViewModel(retryFailedEnrichments = retryFailedEnrichments)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(CaptureUiIntent.RetryFailed)
            assertThat(awaitItem())
                .isEqualTo(CaptureUiEffect.EnqueueEnrichment(listOf("card-a", "card-b")))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry with nothing failed enqueues nothing`() = runTest {
        val retryFailedEnrichments = mockk<RetryFailedEnrichmentsUseCase>()
        coEvery { retryFailedEnrichments() } returns emptyList()
        val viewModel = buildViewModel(retryFailedEnrichments = retryFailedEnrichments)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(CaptureUiIntent.RetryFailed)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the backlog counts reach the state`() = runTest {
        val viewModel = buildViewModel(backlog = EnrichmentBacklog(pending = 3, failed = 2))
        advanceUntilIdle()

        assertThat(viewModel.state.value.pending).isEqualTo(3)
        assertThat(viewModel.state.value.failed).isEqualTo(2)
    }

    @Test
    fun `submit prepends a recent capture pending the word`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        coEvery { captureFlashcard(any(), any()) } returns CARD_ID
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))
        viewModel.onIntent(CaptureUiIntent.Submit)
        advanceUntilIdle()

        val recentCapture: RecentCapture = viewModel.state.value.recentCaptures.first()
        assertThat(recentCapture.flashcardId).isEqualTo(CARD_ID)
        assertThat(recentCapture.word).isEqualTo("borrow")
        assertThat(recentCapture.status).isEqualTo(EnrichmentStatus.PENDING)
    }

    @Test
    fun `a library update flips a recent capture to enriched`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        coEvery { captureFlashcard(any(), any()) } returns CARD_ID
        val libraryRepository = FakeLibraryRepository()
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard, libraryRepository = libraryRepository)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))
        viewModel.onIntent(CaptureUiIntent.Submit)
        advanceUntilIdle()

        libraryRepository.emit(libraryFlashcard(id = CARD_ID, status = EnrichmentStatus.ENRICHED))
        advanceUntilIdle()

        assertThat(viewModel.state.value.recentCaptures.first().status).isEqualTo(EnrichmentStatus.ENRICHED)
    }

    private fun buildViewModel(
        captureFlashcard: CaptureFlashcardUseCase = mockk(),
        retryFailedEnrichments: RetryFailedEnrichmentsUseCase = mockk(),
        backlog: EnrichmentBacklog = EnrichmentBacklog(),
        libraryRepository: LibraryRepository = FakeLibraryRepository(),
    ): CaptureViewModel {
        val enrichmentRepository = mockk<FlashcardEnrichmentRepository>()
        every { enrichmentRepository.observeBacklog() } returns flowOf(backlog)

        val deckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        every { deckSelectionRepository.getDefaultDeckId() } returns DECK_ID

        val getDecksUseCase = mockk<GetDecksUseCase>()
        every { getDecksUseCase() } returns flowOf(listOf(deck()))

        return CaptureViewModel(
            captureFlashcard = captureFlashcard,
            retryFailedEnrichments = retryFailedEnrichments,
            enrichmentRepository = enrichmentRepository,
            defaultDeckSelectionRepository = deckSelectionRepository,
            getDecksUseCase = getDecksUseCase,
            libraryRepository = libraryRepository,
        )
    }

    private fun deck(): Deck = Deck(
        id = DECK_ID,
        name = "Primeras palabras",
        description = "",
        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        cards = emptyList(),
        cardsCount = 0L,
    )

    private fun libraryFlashcard(id: FlashcardId, status: EnrichmentStatus): LibraryFlashcard = LibraryFlashcard(
        id = id,
        deckId = DECK_ID,
        deckName = "Primeras palabras",
        word = "borrow",
        translation = "prestar",
        meaning = "",
        enrichmentStatus = status,
        nextReviewAt = null,
    )

    private class FakeLibraryRepository(
        private val cards: MutableStateFlow<List<LibraryFlashcard>> = MutableStateFlow(emptyList()),
    ) : LibraryRepository {
        override fun observeLibrary(): Flow<List<LibraryFlashcard>> = cards

        fun emit(vararg flashcards: LibraryFlashcard) {
            cards.value = flashcards.toList()
        }
    }

    private companion object {
        val DECK_ID: DeckId = "deck-1".toDeckId()
        val CARD_ID: FlashcardId = "card-1".toFlashcardId()
    }
}

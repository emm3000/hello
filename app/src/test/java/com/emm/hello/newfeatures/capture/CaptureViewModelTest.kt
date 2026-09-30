package com.emm.hello.newfeatures.capture

import app.cash.turbine.test
import com.emm.domain.authoring.CaptureFlashcardUseCase
import com.emm.domain.authoring.CreateManualFlashcardUseCase
import com.emm.domain.authoring.RetryFailedEnrichmentsUseCase
import com.emm.domain.connectivity.ConnectivityRepository
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.ResolveCaptureDeckUseCase
import com.emm.domain.flashcard.EnrichmentBacklog
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.FlashcardEnrichmentRepository
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.generation.GenerationRefusalCode
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
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
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
    fun `submit enqueues enrichment for the captured card and shows no message`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        coEvery { captureFlashcard(any(), any()) } returns CARD_ID
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))

        viewModel.effect.test {
            viewModel.onIntent(CaptureUiIntent.Submit)
            assertThat(awaitItem()).isEqualTo(CaptureUiEffect.EnqueueEnrichment(listOf("card-1")))
            expectNoEvents()
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
    fun `a duplicate word marks the word field, keeps the word and emits nothing`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        coEvery { captureFlashcard(any(), any()) } throws DomainValidationException(
            issues = listOf(ValidationIssue.Error(code = IssueCode.DuplicateWordInDeck, field = "word")),
        )
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))

        viewModel.effect.test {
            viewModel.onIntent(CaptureUiIntent.Submit)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        val state: CaptureUiState = viewModel.state.value
        assertThat(state.wordErrorRes).isEqualTo(R.string.capture_error_duplicate)
        assertThat(state.word).isEqualTo("borrow")
        assertThat(state.isSaving).isFalse()
    }

    @Test
    fun `changing the word clears the duplicate error`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        coEvery { captureFlashcard(any(), any()) } throws DomainValidationException(
            issues = listOf(ValidationIssue.Error(code = IssueCode.DuplicateWordInDeck, field = "word")),
        )
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))
        viewModel.onIntent(CaptureUiIntent.Submit)
        advanceUntilIdle()
        viewModel.onIntent(CaptureUiIntent.WordChanged("borrows"))

        assertThat(viewModel.state.value.wordErrorRes).isNull()
    }

    @Test
    fun `submit while a save is running is ignored`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        val saveGate = CompletableDeferred<FlashcardId>()
        coEvery { captureFlashcard(any(), any()) } coAnswers { saveGate.await() }
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))
        viewModel.onIntent(CaptureUiIntent.Submit)
        viewModel.onIntent(CaptureUiIntent.Submit)
        saveGate.complete(CARD_ID)
        advanceUntilIdle()

        coVerify(exactly = 1) { captureFlashcard(any(), any()) }
    }

    @Test
    fun `submit with a blank word is ignored`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(CaptureUiIntent.Submit)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { captureFlashcard(any(), any()) }
        assertThat(viewModel.state.value.wordErrorRes).isNull()
    }

    @Test
    fun `clicking a recent capture opens its card in its deck`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        coEvery { captureFlashcard(any(), any()) } returns CARD_ID
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))

        viewModel.effect.test {
            viewModel.onIntent(CaptureUiIntent.Submit)
            assertThat(awaitItem()).isEqualTo(CaptureUiEffect.EnqueueEnrichment(listOf("card-1")))
            viewModel.onIntent(CaptureUiIntent.RecentCaptureClicked(CARD_ID))
            assertThat(awaitItem()).isEqualTo(CaptureUiEffect.OpenCard(cardId = "card-1", deckId = "deck-1"))
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

    @Test
    fun `a library update carries the deck of the card to the recent capture`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        coEvery { captureFlashcard(any(), any()) } returns CARD_ID
        val libraryRepository = FakeLibraryRepository()
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard, libraryRepository = libraryRepository)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))
        viewModel.onIntent(CaptureUiIntent.Submit)
        advanceUntilIdle()

        libraryRepository.emit(
            libraryFlashcard(id = CARD_ID, status = EnrichmentStatus.PENDING, deckId = NEWEST_DECK_ID),
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.recentCaptures.first().deckId).isEqualTo(NEWEST_DECK_ID)
    }

    @Test
    fun `a library update carries the failure reason to the recent capture`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        coEvery { captureFlashcard(any(), any()) } returns CARD_ID
        val libraryRepository = FakeLibraryRepository()
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard, libraryRepository = libraryRepository)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))
        viewModel.onIntent(CaptureUiIntent.Submit)
        advanceUntilIdle()

        libraryRepository.emit(
            libraryFlashcard(
                id = CARD_ID,
                status = EnrichmentStatus.FAILED,
                failure = EnrichmentFailure(null, "No pude entender esa entrada."),
            ),
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.recentCaptures.first().failure)
            .isEqualTo(EnrichmentFailure(null, "No pude entender esa entrada."))
    }

    @Test
    fun `a library update carries the failure code to the recent capture`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        coEvery { captureFlashcard(any(), any()) } returns CARD_ID
        val libraryRepository = FakeLibraryRepository()
        val viewModel = buildViewModel(captureFlashcard = captureFlashcard, libraryRepository = libraryRepository)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("borrow"))
        viewModel.onIntent(CaptureUiIntent.Submit)
        advanceUntilIdle()

        libraryRepository.emit(
            libraryFlashcard(
                id = CARD_ID,
                status = EnrichmentStatus.FAILED,
                failure = EnrichmentFailure(GenerationRefusalCode.Unintelligible, "raw message"),
            ),
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.recentCaptures.first().failure?.code)
            .isEqualTo(GenerationRefusalCode.Unintelligible)
    }

    @Test
    fun `state starts online`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isOnline).isTrue()
    }

    @Test
    fun `going offline is reflected in state`() = runTest {
        val connectivityRepository = FakeConnectivityRepository()
        val viewModel = buildViewModel(connectivityRepository = connectivityRepository)
        advanceUntilIdle()

        connectivityRepository.setOnline(false)
        advanceUntilIdle()

        assertThat(viewModel.state.value.isOnline).isFalse()
    }

    @Test
    fun `coming back online is reflected in state`() = runTest {
        val connectivityRepository = FakeConnectivityRepository()
        val viewModel = buildViewModel(connectivityRepository = connectivityRepository)
        advanceUntilIdle()

        connectivityRepository.setOnline(false)
        advanceUntilIdle()
        connectivityRepository.setOnline(true)
        advanceUntilIdle()

        assertThat(viewModel.state.value.isOnline).isTrue()
    }

    @Test
    fun `manual submit creates an enriched card and emits nothing`() = runTest {
        val captureFlashcard = mockk<CaptureFlashcardUseCase>()
        val createManualFlashcard = mockk<CreateManualFlashcardUseCase>()
        coEvery { createManualFlashcard(any(), any(), any(), any()) } returns CARD_ID
        val viewModel = buildViewModel(
            captureFlashcard = captureFlashcard,
            createManualFlashcard = createManualFlashcard,
        )
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("give up"))
        viewModel.onIntent(CaptureUiIntent.ManualModeSelected(true))
        viewModel.onIntent(CaptureUiIntent.TranslationChanged("rendirse"))

        viewModel.effect.test {
            viewModel.onIntent(CaptureUiIntent.Submit)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            createManualFlashcard(
                deckId = DECK_ID,
                word = "give up",
                translation = "rendirse",
                meaning = "",
            )
        }
        coVerify(exactly = 0) { captureFlashcard(any(), any()) }
        assertThat(viewModel.state.value.recentCaptures.first().status).isEqualTo(EnrichmentStatus.ENRICHED)
    }

    @Test
    fun `manual mode blocks submit until the translation is filled`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("give up"))
        viewModel.onIntent(CaptureUiIntent.ManualModeSelected(true))

        assertThat(viewModel.state.value.canSubmit).isFalse()

        viewModel.onIntent(CaptureUiIntent.TranslationChanged("rendirse"))

        assertThat(viewModel.state.value.canSubmit).isTrue()
    }

    @Test
    fun `manual submit clears the fields and keeps manual mode`() = runTest {
        val createManualFlashcard = mockk<CreateManualFlashcardUseCase>()
        coEvery { createManualFlashcard(any(), any(), any(), any()) } returns CARD_ID
        val viewModel = buildViewModel(createManualFlashcard = createManualFlashcard)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("give up"))
        viewModel.onIntent(CaptureUiIntent.ManualModeSelected(true))
        viewModel.onIntent(CaptureUiIntent.TranslationChanged("rendirse"))
        viewModel.onIntent(CaptureUiIntent.MeaningChanged("to stop trying"))
        viewModel.onIntent(CaptureUiIntent.Submit)
        advanceUntilIdle()

        val state: CaptureUiState = viewModel.state.value
        assertThat(state.word).isEmpty()
        assertThat(state.translation).isEmpty()
        assertThat(state.meaning).isEmpty()
        assertThat(state.isManual).isTrue()
    }

    @Test
    fun `manual duplicate marks the word field`() = runTest {
        val createManualFlashcard = mockk<CreateManualFlashcardUseCase>()
        coEvery { createManualFlashcard(any(), any(), any(), any()) } throws DomainValidationException(
            issues = listOf(ValidationIssue.Error(code = IssueCode.DuplicateWordInDeck, field = "word")),
        )
        val viewModel = buildViewModel(createManualFlashcard = createManualFlashcard)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("give up"))
        viewModel.onIntent(CaptureUiIntent.ManualModeSelected(true))
        viewModel.onIntent(CaptureUiIntent.TranslationChanged("rendirse"))

        viewModel.effect.test {
            viewModel.onIntent(CaptureUiIntent.Submit)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(viewModel.state.value.wordErrorRes).isEqualTo(R.string.capture_error_duplicate)
        assertThat(viewModel.state.value.word).isEqualTo("give up")
    }

    @Test
    fun `manual submit without a translation marks the translation field`() = runTest {
        val createManualFlashcard = mockk<CreateManualFlashcardUseCase>()
        val viewModel = buildViewModel(createManualFlashcard = createManualFlashcard)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("give up"))
        viewModel.onIntent(CaptureUiIntent.ManualModeSelected(true))
        viewModel.onIntent(CaptureUiIntent.Submit)
        advanceUntilIdle()

        assertThat(viewModel.state.value.translationErrorRes).isEqualTo(R.string.capture_error_translation_required)
        coVerify(exactly = 0) { createManualFlashcard(any(), any(), any(), any()) }

        viewModel.onIntent(CaptureUiIntent.TranslationChanged("rendirse"))

        assertThat(viewModel.state.value.translationErrorRes).isNull()
    }

    @Test
    fun `leaving manual mode clears the manual fields`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("give up"))
        viewModel.onIntent(CaptureUiIntent.ManualModeSelected(true))
        viewModel.onIntent(CaptureUiIntent.TranslationChanged("rendirse"))
        viewModel.onIntent(CaptureUiIntent.MeaningChanged("to stop trying"))
        viewModel.onIntent(CaptureUiIntent.ManualModeSelected(false))

        val state: CaptureUiState = viewModel.state.value
        assertThat(state.isManual).isFalse()
        assertThat(state.translation).isEmpty()
        assertThat(state.meaning).isEmpty()
        assertThat(state.word).isEqualTo("give up")
    }

    @Test
    fun `selecting manual mode again keeps the manual fields`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.WordChanged("give up"))
        viewModel.onIntent(CaptureUiIntent.ManualModeSelected(true))
        viewModel.onIntent(CaptureUiIntent.TranslationChanged("rendirse"))
        viewModel.onIntent(CaptureUiIntent.ManualModeSelected(true))

        val state: CaptureUiState = viewModel.state.value
        assertThat(state.isManual).isTrue()
        assertThat(state.translation).isEqualTo("rendirse")
    }

    @Test
    fun `with no default selected the target deck is the oldest one and not the newest`() = runTest {
        val viewModel = buildViewModel(decks = listOf(newestDeck(), deck()), defaultDeckId = null)
        advanceUntilIdle()

        assertThat(viewModel.state.value.targetDeck?.id).isEqualTo(DECK_ID)
    }

    @Test
    fun `selecting a deck stores it as the default and moves the target deck`() = runTest {
        val deckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        every { deckSelectionRepository.setDefaultDeckId(any()) } just Runs
        val viewModel = buildViewModel(
            decks = listOf(newestDeck(), deck()),
            defaultDeckId = null,
            deckSelectionRepository = deckSelectionRepository,
        )
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.DeckSelected(NEWEST_DECK_ID))

        verify { deckSelectionRepository.setDefaultDeckId(NEWEST_DECK_ID) }
        assertThat(viewModel.state.value.targetDeck?.id).isEqualTo(NEWEST_DECK_ID)
    }

    @Test
    fun `selecting a deck closes the picker`() = runTest {
        val deckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        every { deckSelectionRepository.setDefaultDeckId(any()) } just Runs
        val viewModel = buildViewModel(
            decks = listOf(newestDeck(), deck()),
            defaultDeckId = null,
            deckSelectionRepository = deckSelectionRepository,
        )
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.DeckPickerOpened)
        viewModel.onIntent(CaptureUiIntent.DeckSelected(NEWEST_DECK_ID))

        assertThat(viewModel.state.value.isDeckPickerOpen).isFalse()
    }

    @Test
    fun `selecting an unknown deck leaves the target deck unchanged`() = runTest {
        val deckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        every { deckSelectionRepository.setDefaultDeckId(any()) } just Runs
        val viewModel = buildViewModel(
            decks = listOf(newestDeck(), deck()),
            defaultDeckId = null,
            deckSelectionRepository = deckSelectionRepository,
        )
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.DeckPickerOpened)
        viewModel.onIntent(CaptureUiIntent.DeckSelected(UNKNOWN_DECK_ID))

        assertThat(viewModel.state.value.targetDeck?.id).isEqualTo(DECK_ID)
        assertThat(viewModel.state.value.isDeckPickerOpen).isFalse()
    }

    @Test
    fun `opening and dismissing the picker flips the picker flag`() = runTest {
        val viewModel = buildViewModel(decks = listOf(newestDeck(), deck()), defaultDeckId = null)
        advanceUntilIdle()

        viewModel.onIntent(CaptureUiIntent.DeckPickerOpened)
        assertThat(viewModel.state.value.isDeckPickerOpen).isTrue()

        viewModel.onIntent(CaptureUiIntent.DeckPickerDismissed)
        assertThat(viewModel.state.value.isDeckPickerOpen).isFalse()
    }

    @Test
    fun `the decks reach the state so the picker can list them`() = runTest {
        val viewModel = buildViewModel(decks = listOf(newestDeck(), deck()), defaultDeckId = null)
        advanceUntilIdle()

        assertThat(viewModel.state.value.decks.map { it.id }).containsExactly(NEWEST_DECK_ID, DECK_ID).inOrder()
    }

    @Test
    fun `installed curated decks stay out of the picker when a user deck exists`() = runTest {
        val curatedDeck: Deck = deck(id = CURATED_DECK_ID, name = "Phrasal verbs")
        val viewModel = buildViewModel(decks = listOf(curatedDeck, deck()), defaultDeckId = CURATED_DECK_ID)
        advanceUntilIdle()

        assertThat(viewModel.state.value.decks.map { it.id }).containsExactly(DECK_ID)
        assertThat(viewModel.state.value.targetDeck?.id).isEqualTo(DECK_ID)
    }

    private fun buildViewModel(
        captureFlashcard: CaptureFlashcardUseCase = mockk(),
        createManualFlashcard: CreateManualFlashcardUseCase = mockk(),
        retryFailedEnrichments: RetryFailedEnrichmentsUseCase = mockk(),
        backlog: EnrichmentBacklog = EnrichmentBacklog(),
        libraryRepository: LibraryRepository = FakeLibraryRepository(),
        connectivityRepository: ConnectivityRepository = FakeConnectivityRepository(),
        decks: List<Deck> = listOf(deck()),
        defaultDeckId: DeckId? = DECK_ID,
        deckSelectionRepository: DefaultDeckSelectionRepository = mockk(),
    ): CaptureViewModel {
        val enrichmentRepository = mockk<FlashcardEnrichmentRepository>()
        every { enrichmentRepository.observeBacklog() } returns flowOf(backlog)

        every { deckSelectionRepository.getDefaultDeckId() } returns defaultDeckId

        val getDecksUseCase = mockk<GetDecksUseCase>()
        every { getDecksUseCase() } returns flowOf(decks)

        return CaptureViewModel(
            captureFlashcard = captureFlashcard,
            createManualFlashcard = createManualFlashcard,
            retryFailedEnrichments = retryFailedEnrichments,
            enrichmentRepository = enrichmentRepository,
            defaultDeckSelectionRepository = deckSelectionRepository,
            resolveCaptureDeck = ResolveCaptureDeckUseCase(deckSelectionRepository),
            getDecksUseCase = getDecksUseCase,
            libraryRepository = libraryRepository,
            connectivityRepository = connectivityRepository,
        )
    }

    private fun deck(
        id: DeckId = DECK_ID,
        name: String = "Primeras palabras",
        createdAt: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0),
    ): Deck = Deck(
        id = id,
        name = name,
        description = "",
        createdAt = createdAt,
        cards = emptyList(),
        cardsCount = 0L,
    )

    private fun newestDeck(): Deck = deck(
        id = NEWEST_DECK_ID,
        name = "Job interview",
        createdAt = LocalDateTime.of(2026, 6, 1, 0, 0),
    )

    private fun libraryFlashcard(
        id: FlashcardId,
        status: EnrichmentStatus,
        failure: EnrichmentFailure? = null,
        deckId: DeckId = DECK_ID,
    ): LibraryFlashcard = LibraryFlashcard(
        id = id,
        deckId = deckId,
        deckName = "Primeras palabras",
        word = "borrow",
        translation = "prestar",
        meaning = "",
        enrichmentStatus = status,
        enrichmentFailure = failure,
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

    private class FakeConnectivityRepository(
        private val online: MutableStateFlow<Boolean> = MutableStateFlow(true),
    ) : ConnectivityRepository {
        override fun observeOnline(): Flow<Boolean> = online

        fun setOnline(value: Boolean) {
            online.value = value
        }
    }

    private companion object {
        val DECK_ID: DeckId = "deck-1".toDeckId()
        val NEWEST_DECK_ID: DeckId = "deck-2".toDeckId()
        val UNKNOWN_DECK_ID: DeckId = "deck-missing".toDeckId()
        val CURATED_DECK_ID: DeckId = "curated-phrasal-verbs".toDeckId()
        val CARD_ID: FlashcardId = "card-1".toFlashcardId()
    }
}

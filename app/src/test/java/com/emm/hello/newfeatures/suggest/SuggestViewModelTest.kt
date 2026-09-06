package com.emm.hello.newfeatures.suggest

import app.cash.turbine.test
import com.emm.domain.authoring.CaptureFlashcardUseCase
import com.emm.domain.connectivity.ConnectivityRepository
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.suggestion.SuggestWordsUseCase
import com.emm.domain.suggestion.SuggestedWord
import com.emm.domain.suggestion.WordSuggestions
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
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SuggestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `load success populates the situation and the words`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.loadFailed).isFalse()
        assertThat(viewModel.state.value.situation).isEqualTo(SITUATION)
        assertThat(viewModel.state.value.words).isEqualTo(listOf(WORD_A, WORD_B))
    }

    @Test
    fun `load failure sets loadFailed`() = runTest {
        val suggestWordsUseCase = mockk<SuggestWordsUseCase>()
        coEvery { suggestWordsUseCase() } throws RuntimeException("boom")
        val viewModel = buildViewModel(suggestWordsUseCase = suggestWordsUseCase)
        advanceUntilIdle()

        assertThat(viewModel.state.value.loadFailed).isTrue()
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun `retry reloads`() = runTest {
        val suggestWordsUseCase = mockk<SuggestWordsUseCase>()
        coEvery { suggestWordsUseCase() } returns WordSuggestions(SITUATION, listOf(WORD_A))
        val viewModel = buildViewModel(suggestWordsUseCase = suggestWordsUseCase)
        advanceUntilIdle()

        viewModel.onIntent(SuggestUiIntent.Retry)
        advanceUntilIdle()

        coVerify(exactly = 2) { suggestWordsUseCase() }
    }

    @Test
    fun `toggling a word selects it then deselects it`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onIntent(SuggestUiIntent.WordToggled(WORD_A.word))
        assertThat(viewModel.state.value.selectedWords).containsExactly(WORD_A.word)

        viewModel.onIntent(SuggestUiIntent.WordToggled(WORD_A.word))
        assertThat(viewModel.state.value.selectedWords).isEmpty()
    }

    @Test
    fun `add selected captures each word with its translation and emits enqueue then message then navigate back`() =
        runTest {
            val captureFlashcardUseCase = mockk<CaptureFlashcardUseCase>()
            coEvery {
                captureFlashcardUseCase(deckId = DECK_ID, word = WORD_A.word, translation = WORD_A.translation)
            } returns CARD_ID_A
            coEvery {
                captureFlashcardUseCase(deckId = DECK_ID, word = WORD_B.word, translation = WORD_B.translation)
            } returns CARD_ID_B
            val viewModel = buildViewModel(captureFlashcardUseCase = captureFlashcardUseCase)
            advanceUntilIdle()

            viewModel.onIntent(SuggestUiIntent.WordToggled(WORD_A.word))
            viewModel.onIntent(SuggestUiIntent.WordToggled(WORD_B.word))

            viewModel.effect.test {
                viewModel.onIntent(SuggestUiIntent.AddSelected)
                assertThat(awaitItem()).isEqualTo(SuggestUiEffect.EnqueueEnrichment(listOf("card-a", "card-b")))
                assertThat(awaitItem()).isEqualTo(SuggestUiEffect.ShowMessage(R.string.suggest_added))
                assertThat(awaitItem()).isEqualTo(SuggestUiEffect.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a duplicate word is skipped but the others are still added`() = runTest {
        val captureFlashcardUseCase = mockk<CaptureFlashcardUseCase>()
        coEvery {
            captureFlashcardUseCase(deckId = DECK_ID, word = WORD_A.word, translation = WORD_A.translation)
        } throws DomainValidationException(
            issues = listOf(ValidationIssue.Error(code = IssueCode.DuplicateWordInDeck, field = "word")),
        )
        coEvery {
            captureFlashcardUseCase(deckId = DECK_ID, word = WORD_B.word, translation = WORD_B.translation)
        } returns CARD_ID_B
        val viewModel = buildViewModel(captureFlashcardUseCase = captureFlashcardUseCase)
        advanceUntilIdle()

        viewModel.onIntent(SuggestUiIntent.WordToggled(WORD_A.word))
        viewModel.onIntent(SuggestUiIntent.WordToggled(WORD_B.word))

        viewModel.effect.test {
            viewModel.onIntent(SuggestUiIntent.AddSelected)
            assertThat(awaitItem()).isEqualTo(SuggestUiEffect.EnqueueEnrichment(listOf("card-b")))
            assertThat(awaitItem()).isEqualTo(SuggestUiEffect.ShowMessage(R.string.suggest_added))
            assertThat(awaitItem()).isEqualTo(SuggestUiEffect.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `add selected with no deck shows the no-deck message and does not capture`() = runTest {
        val captureFlashcardUseCase = mockk<CaptureFlashcardUseCase>()
        val viewModel = buildViewModel(
            captureFlashcardUseCase = captureFlashcardUseCase,
            decks = emptyList(),
            defaultDeckId = null,
        )
        advanceUntilIdle()

        viewModel.onIntent(SuggestUiIntent.WordToggled(WORD_A.word))

        viewModel.effect.test {
            viewModel.onIntent(SuggestUiIntent.AddSelected)
            assertThat(awaitItem()).isEqualTo(SuggestUiEffect.ShowMessage(R.string.suggest_error_no_deck))
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { captureFlashcardUseCase(any(), any(), any()) }
    }

    @Test
    fun `offline skips the request and reports offline`() = runTest {
        val suggestWordsUseCase = defaultSuggestWordsUseCase()
        val connectivityRepository = FakeConnectivityRepository(online = false)
        val viewModel = buildViewModel(
            suggestWordsUseCase = suggestWordsUseCase,
            connectivityRepository = connectivityRepository,
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { suggestWordsUseCase() }
        assertThat(viewModel.state.value.isOffline).isTrue()
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.loadFailed).isFalse()
    }

    @Test
    fun `retry after coming back online loads the suggestions`() = runTest {
        val suggestWordsUseCase = defaultSuggestWordsUseCase()
        val connectivityRepository = FakeConnectivityRepository(online = false)
        val viewModel = buildViewModel(
            suggestWordsUseCase = suggestWordsUseCase,
            connectivityRepository = connectivityRepository,
        )
        advanceUntilIdle()

        connectivityRepository.setOnline(true)
        viewModel.onIntent(SuggestUiIntent.Retry)
        advanceUntilIdle()

        coVerify(exactly = 1) { suggestWordsUseCase() }
        assertThat(viewModel.state.value.isOffline).isFalse()
        assertThat(viewModel.state.value.words).isEqualTo(listOf(WORD_A, WORD_B))
    }

    private fun buildViewModel(
        suggestWordsUseCase: SuggestWordsUseCase = defaultSuggestWordsUseCase(),
        captureFlashcardUseCase: CaptureFlashcardUseCase = mockk(),
        decks: List<Deck> = listOf(deck()),
        defaultDeckId: DeckId? = DECK_ID,
        connectivityRepository: ConnectivityRepository = FakeConnectivityRepository(),
    ): SuggestViewModel {
        val getDecksUseCase = mockk<GetDecksUseCase>()
        every { getDecksUseCase() } returns flowOf(decks)

        val defaultDeckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        every { defaultDeckSelectionRepository.getDefaultDeckId() } returns defaultDeckId

        return SuggestViewModel(
            suggestWordsUseCase = suggestWordsUseCase,
            captureFlashcardUseCase = captureFlashcardUseCase,
            getDecksUseCase = getDecksUseCase,
            defaultDeckSelectionRepository = defaultDeckSelectionRepository,
            connectivityRepository = connectivityRepository,
        )
    }

    private fun defaultSuggestWordsUseCase(): SuggestWordsUseCase {
        val useCase = mockk<SuggestWordsUseCase>()
        coEvery { useCase() } returns WordSuggestions(SITUATION, listOf(WORD_A, WORD_B))
        return useCase
    }

    private fun deck(): Deck = Deck(
        id = DECK_ID,
        name = "Primeras palabras",
        description = "",
        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        cards = emptyList(),
        cardsCount = 0L,
    )

    private class FakeConnectivityRepository(online: Boolean = true) : ConnectivityRepository {
        private val online: MutableStateFlow<Boolean> = MutableStateFlow(online)

        override fun observeOnline(): Flow<Boolean> = online

        fun setOnline(value: Boolean) {
            online.value = value
        }
    }

    private companion object {
        const val SITUATION: String = "At a coffee shop"
        val WORD_A: SuggestedWord = SuggestedWord(word = "borrow", translation = "prestar")
        val WORD_B: SuggestedWord = SuggestedWord(word = "receipt", translation = "recibo")
        val DECK_ID: DeckId = "deck-1".toDeckId()
        val CARD_ID_A: FlashcardId = "card-a".toFlashcardId()
        val CARD_ID_B: FlashcardId = "card-b".toFlashcardId()
    }
}

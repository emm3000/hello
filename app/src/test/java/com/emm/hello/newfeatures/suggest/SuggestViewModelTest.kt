package com.emm.hello.newfeatures.suggest

import app.cash.turbine.test
import com.emm.domain.authoring.CaptureFlashcardUseCase
import com.emm.domain.connectivity.ConnectivityRepository
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.suggestion.ObserveSuggestedWordsUseCase
import com.emm.domain.suggestion.RefreshSuggestedWordsUseCase
import com.emm.domain.suggestion.SuggestedWord
import com.emm.domain.suggestion.SuggestedWordsRefresher
import com.emm.domain.suggestion.WordSuggestionCache
import com.emm.domain.suggestion.WordSuggestionRepository
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SuggestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `a cached pool is shown without asking the backend`() = runTest {
        val suggestionRepository = FakeWordSuggestionRepository(WordSuggestions(OTHER_SITUATION, listOf(WORD_C)))
        val viewModel: SuggestViewModel = buildViewModel(this, suggestionRepository = suggestionRepository)
        runCurrent()

        assertThat(suggestionRepository.calls).isEqualTo(0)
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.loadFailed).isFalse()
        assertThat(viewModel.state.value.situation).isEqualTo(SITUATION)
        assertThat(viewModel.state.value.words).isEqualTo(listOf(WORD_A, WORD_B))
    }

    @Test
    fun `an empty cache asks the backend once and shows the words`() = runTest {
        val suggestionRepository = FakeWordSuggestionRepository(WordSuggestions(SITUATION, listOf(WORD_A, WORD_B)))
        val viewModel: SuggestViewModel = buildViewModel(
            scope = this,
            cache = FakeWordSuggestionCache(),
            suggestionRepository = suggestionRepository,
        )
        runCurrent()

        assertThat(suggestionRepository.calls).isEqualTo(1)
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.words).isEqualTo(listOf(WORD_A, WORD_B))
    }

    @Test
    fun `an empty pool with no decision taken yet keeps the spinner up`() = runTest {
        val viewModel: SuggestViewModel = buildViewModel(
            scope = this,
            cache = FakeWordSuggestionCache(),
            suggestionRepository = FakeWordSuggestionRepository(WordSuggestions(SITUATION, listOf(WORD_A))),
        )

        assertThat(viewModel.state.value.isLoading).isTrue()
        assertThat(viewModel.state.value.loadFailed).isFalse()
        assertThat(viewModel.state.value.isOffline).isFalse()
    }

    @Test
    fun `a refresh that yields no usable words ends with the empty state and no spinner`() = runTest {
        val viewModel: SuggestViewModel = buildViewModel(
            scope = this,
            cache = FakeWordSuggestionCache(),
            suggestionRepository = FakeWordSuggestionRepository(WordSuggestions(SITUATION, emptyList())),
        )
        runCurrent()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.words).isEmpty()
        assertThat(viewModel.state.value.loadFailed).isFalse()
        assertThat(viewModel.state.value.isOffline).isFalse()
    }

    @Test
    fun `an empty cache while offline reports offline without asking the backend`() = runTest {
        val suggestionRepository = FakeWordSuggestionRepository(WordSuggestions(SITUATION, listOf(WORD_A)))
        val viewModel: SuggestViewModel = buildViewModel(
            scope = this,
            cache = FakeWordSuggestionCache(),
            suggestionRepository = suggestionRepository,
            connectivityRepository = FakeConnectivityRepository(online = false),
        )
        runCurrent()

        assertThat(suggestionRepository.calls).isEqualTo(0)
        assertThat(viewModel.state.value.isOffline).isTrue()
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.loadFailed).isFalse()
    }

    @Test
    fun `a backend failure with an empty cache sets loadFailed`() = runTest {
        val viewModel: SuggestViewModel = buildViewModel(
            scope = this,
            cache = FakeWordSuggestionCache(),
            suggestionRepository = FakeWordSuggestionRepository(failure = RuntimeException("boom")),
        )
        runCurrent()

        assertThat(viewModel.state.value.loadFailed).isTrue()
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.isOffline).isFalse()
    }

    @Test
    fun `a backend failure with a cached pool leaves loadFailed false`() = runTest {
        val viewModel: SuggestViewModel = buildViewModel(
            scope = this,
            suggestionRepository = FakeWordSuggestionRepository(failure = RuntimeException("boom")),
        )
        runCurrent()

        viewModel.onIntent(SuggestUiIntent.Retry)
        runCurrent()

        assertThat(viewModel.state.value.loadFailed).isFalse()
        assertThat(viewModel.state.value.words).isEqualTo(listOf(WORD_A, WORD_B))
    }

    @Test
    fun `retry asks for a new batch and swaps the words`() = runTest {
        val suggestionRepository = FakeWordSuggestionRepository(WordSuggestions(OTHER_SITUATION, listOf(WORD_C)))
        val viewModel: SuggestViewModel = buildViewModel(this, suggestionRepository = suggestionRepository)
        runCurrent()

        viewModel.onIntent(SuggestUiIntent.Retry)
        runCurrent()

        assertThat(suggestionRepository.calls).isEqualTo(1)
        assertThat(viewModel.state.value.situation).isEqualTo(OTHER_SITUATION)
        assertThat(viewModel.state.value.words).isEqualTo(listOf(WORD_C))
    }

    @Test
    fun `a swapped batch drops the selections that disappeared`() = runTest {
        val suggestionRepository = FakeWordSuggestionRepository(
            WordSuggestions(OTHER_SITUATION, listOf(WORD_B, WORD_C)),
        )
        val viewModel: SuggestViewModel = buildViewModel(this, suggestionRepository = suggestionRepository)
        runCurrent()

        viewModel.onIntent(SuggestUiIntent.WordToggled(WORD_A.word))
        viewModel.onIntent(SuggestUiIntent.WordToggled(WORD_B.word))
        viewModel.onIntent(SuggestUiIntent.Retry)
        runCurrent()

        assertThat(viewModel.state.value.selectedWords).containsExactly(WORD_B.word)
    }

    @Test
    fun `retry after coming back online loads the suggestions`() = runTest {
        val suggestionRepository = FakeWordSuggestionRepository(WordSuggestions(SITUATION, listOf(WORD_A, WORD_B)))
        val connectivityRepository = FakeConnectivityRepository(online = false)
        val viewModel: SuggestViewModel = buildViewModel(
            scope = this,
            cache = FakeWordSuggestionCache(),
            suggestionRepository = suggestionRepository,
            connectivityRepository = connectivityRepository,
        )
        runCurrent()

        connectivityRepository.setOnline(true)
        viewModel.onIntent(SuggestUiIntent.Retry)
        runCurrent()

        assertThat(suggestionRepository.calls).isEqualTo(1)
        assertThat(viewModel.state.value.isOffline).isFalse()
        assertThat(viewModel.state.value.words).isEqualTo(listOf(WORD_A, WORD_B))
    }

    @Test
    fun `toggling a word selects it then deselects it`() = runTest {
        val viewModel: SuggestViewModel = buildViewModel(this)
        runCurrent()

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
            val viewModel: SuggestViewModel =
                buildViewModel(this, captureFlashcardUseCase = captureFlashcardUseCase)
            runCurrent()

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
        } throws duplicateWordException()
        coEvery {
            captureFlashcardUseCase(deckId = DECK_ID, word = WORD_B.word, translation = WORD_B.translation)
        } returns CARD_ID_B
        val viewModel: SuggestViewModel = buildViewModel(this, captureFlashcardUseCase = captureFlashcardUseCase)
        runCurrent()

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
    fun `add selected with every word already in the deck shows the all-known message and stays`() = runTest {
        val captureFlashcardUseCase = mockk<CaptureFlashcardUseCase>()
        coEvery {
            captureFlashcardUseCase(deckId = DECK_ID, word = WORD_A.word, translation = WORD_A.translation)
        } throws duplicateWordException()
        coEvery {
            captureFlashcardUseCase(deckId = DECK_ID, word = WORD_B.word, translation = WORD_B.translation)
        } throws duplicateWordException()
        val viewModel: SuggestViewModel = buildViewModel(this, captureFlashcardUseCase = captureFlashcardUseCase)
        runCurrent()

        viewModel.onIntent(SuggestUiIntent.WordToggled(WORD_A.word))
        viewModel.onIntent(SuggestUiIntent.WordToggled(WORD_B.word))

        viewModel.effect.test {
            viewModel.onIntent(SuggestUiIntent.AddSelected)
            assertThat(awaitItem()).isEqualTo(SuggestUiEffect.ShowMessage(R.string.suggest_all_known))
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(viewModel.state.value.isAdding).isFalse()
        assertThat(viewModel.state.value.selectedWords).isEmpty()
    }

    @Test
    fun `add selected with no deck shows the no-deck message and does not capture`() = runTest {
        val captureFlashcardUseCase = mockk<CaptureFlashcardUseCase>()
        val viewModel: SuggestViewModel = buildViewModel(
            scope = this,
            captureFlashcardUseCase = captureFlashcardUseCase,
            decks = emptyList(),
            defaultDeckId = null,
        )
        runCurrent()

        viewModel.onIntent(SuggestUiIntent.WordToggled(WORD_A.word))

        viewModel.effect.test {
            viewModel.onIntent(SuggestUiIntent.AddSelected)
            assertThat(awaitItem()).isEqualTo(SuggestUiEffect.ShowMessage(R.string.suggest_error_no_deck))
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { captureFlashcardUseCase(any(), any(), any()) }
    }

    @Test
    fun `with no default selected the words go to the oldest deck and not the newest`() = runTest {
        val captureFlashcardUseCase = mockk<CaptureFlashcardUseCase>()
        coEvery {
            captureFlashcardUseCase(deckId = DECK_ID, word = WORD_A.word, translation = WORD_A.translation)
        } returns CARD_ID_A
        val viewModel: SuggestViewModel = buildViewModel(
            scope = this,
            captureFlashcardUseCase = captureFlashcardUseCase,
            decks = listOf(newestDeck(), deck()),
            defaultDeckId = null,
        )
        runCurrent()

        viewModel.onIntent(SuggestUiIntent.WordToggled(WORD_A.word))
        viewModel.onIntent(SuggestUiIntent.AddSelected)
        runCurrent()

        coVerify {
            captureFlashcardUseCase(deckId = DECK_ID, word = WORD_A.word, translation = WORD_A.translation)
        }
    }

    private fun buildViewModel(
        scope: TestScope,
        cache: WordSuggestionCache = FakeWordSuggestionCache(WordSuggestions(SITUATION, listOf(WORD_A, WORD_B))),
        suggestionRepository: WordSuggestionRepository = FakeWordSuggestionRepository(),
        connectivityRepository: ConnectivityRepository = FakeConnectivityRepository(),
        captureFlashcardUseCase: CaptureFlashcardUseCase = mockk(),
        decks: List<Deck> = listOf(deck()),
        defaultDeckId: DeckId? = DECK_ID,
    ): SuggestViewModel {
        val flashcardRepository = mockk<FlashcardRepository>()
        coEvery { flashcardRepository.fetchRecentWords(any()) } returns emptyList()

        val getDecksUseCase = mockk<GetDecksUseCase>()
        every { getDecksUseCase() } returns flowOf(decks)

        val defaultDeckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        every { defaultDeckSelectionRepository.getDefaultDeckId() } returns defaultDeckId

        val observeSuggestedWords = ObserveSuggestedWordsUseCase(flashcardRepository, cache)

        return SuggestViewModel(
            observeSuggestedWords = observeSuggestedWords,
            refresher = SuggestedWordsRefresher(
                observeSuggestedWords = observeSuggestedWords,
                refreshSuggestedWords = RefreshSuggestedWordsUseCase(
                    flashcardRepository,
                    suggestionRepository,
                    cache,
                ),
                connectivityRepository = connectivityRepository,
                scope = scope.backgroundScope,
            ),
            captureFlashcardUseCase = captureFlashcardUseCase,
            getDecksUseCase = getDecksUseCase,
            defaultDeckSelectionRepository = defaultDeckSelectionRepository,
        )
    }

    private fun duplicateWordException(): DomainValidationException = DomainValidationException(
        issues = listOf(ValidationIssue.Error(code = IssueCode.DuplicateWordInDeck, field = "word")),
    )

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

    private class FakeConnectivityRepository(online: Boolean = true) : ConnectivityRepository {
        private val online: MutableStateFlow<Boolean> = MutableStateFlow(online)

        override fun observeOnline(): Flow<Boolean> = online

        fun setOnline(value: Boolean) {
            this.online.value = value
        }
    }

    private class FakeWordSuggestionCache(initial: WordSuggestions? = null) : WordSuggestionCache {
        private val stored: MutableStateFlow<WordSuggestions?> = MutableStateFlow(initial)

        override fun observe(): Flow<WordSuggestions?> = stored

        override suspend fun replace(suggestions: WordSuggestions) {
            stored.value = suggestions
        }
    }

    private class FakeWordSuggestionRepository(
        private val result: WordSuggestions = WordSuggestions(situation = "", words = emptyList()),
        private val failure: Throwable? = null,
    ) : WordSuggestionRepository {

        var calls: Int = 0
            private set

        override suspend fun suggest(recentWords: List<String>): WordSuggestions {
            calls += 1
            failure?.let { throw it }
            return result
        }
    }

    private companion object {
        const val SITUATION: String = "At a coffee shop"
        const val OTHER_SITUATION: String = "At the airport"
        val WORD_A: SuggestedWord = SuggestedWord(word = "borrow", translation = "prestar")
        val WORD_B: SuggestedWord = SuggestedWord(word = "receipt", translation = "recibo")
        val WORD_C: SuggestedWord = SuggestedWord(word = "gate", translation = "puerta")
        val DECK_ID: DeckId = "deck-1".toDeckId()
        val NEWEST_DECK_ID: DeckId = "deck-2".toDeckId()
        val CARD_ID_A: FlashcardId = "card-a".toFlashcardId()
        val CARD_ID_B: FlashcardId = "card-b".toFlashcardId()
    }
}

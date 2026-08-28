package com.emm.hello.newfeatures.library

import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.RestoreFlashcardUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.library.LibraryFlashcard
import com.emm.domain.library.LibraryRepository
import com.emm.domain.library.SearchLibraryUseCase
import com.emm.domain.time.Clock
import com.emm.hello.MainDispatcherRule
import com.emm.hello.newfeatures.shared.UndoEventHolder
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `a library emission lands in state with the clock's reference time`() = runTest {
        val libraryRepository = FakeLibraryRepository()
        val viewModel = buildViewModel(libraryRepository = libraryRepository)

        libraryRepository.emit(libraryFlashcard(id = CARD_ID, word = "borrow"))
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.referenceNow).isEqualTo(FIXED_NOW)
        assertThat(viewModel.state.value.cards.map { card -> card.word }).containsExactly("borrow")
    }

    @Test
    fun `query changed filters the cards after the debounce`() = runTest {
        val libraryRepository = FakeLibraryRepository()
        val viewModel = buildViewModel(libraryRepository = libraryRepository)
        libraryRepository.emit(
            libraryFlashcard(id = CARD_ID, word = "borrow"),
            libraryFlashcard(id = CARD_ID_2, word = "compelling"),
        )
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
        advanceUntilIdle()

        viewModel.onIntent(LibraryUiIntent.QueryChanged("borrow"))
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
        advanceUntilIdle()

        assertThat(viewModel.state.value.cards.map { card -> card.word }).containsExactly("borrow")
    }

    @Test
    fun `a card with no scheduled review is new`() {
        val card = libraryFlashcard(id = CARD_ID, word = "borrow", nextReviewAt = null)

        assertThat(card.scheduleStatus(now = LIMA_NOW, zone = LIMA_ZONE)).isEqualTo(ScheduleStatus.New)
    }

    @Test
    fun `a review an hour in the past is due today`() {
        val reviewAt: Instant = LIMA_NOW.minus(1, ChronoUnit.HOURS)
        val card = libraryFlashcard(id = CARD_ID, word = "borrow", nextReviewAt = reviewAt.toEpochMilli())

        assertThat(card.scheduleStatus(now = LIMA_NOW, zone = LIMA_ZONE)).isEqualTo(ScheduleStatus.DueToday)
    }

    @Test
    fun `a review later the same calendar day is due today`() {
        val reviewAt: Instant = LIMA_NOW.plus(2, ChronoUnit.HOURS)
        val card = libraryFlashcard(id = CARD_ID, word = "borrow", nextReviewAt = reviewAt.toEpochMilli())

        assertThat(card.scheduleStatus(now = LIMA_NOW, zone = LIMA_ZONE)).isEqualTo(ScheduleStatus.DueToday)
    }

    @Test
    fun `a review the next calendar day is in 1 day`() {
        val reviewAt: Instant = LIMA_NOW.plus(1, ChronoUnit.DAYS)
        val card = libraryFlashcard(id = CARD_ID, word = "borrow", nextReviewAt = reviewAt.toEpochMilli())

        assertThat(card.scheduleStatus(now = LIMA_NOW, zone = LIMA_ZONE)).isEqualTo(ScheduleStatus.InDays(1))
    }

    @Test
    fun `a review 3 days out is in 3 days`() {
        val reviewAt: Instant = LIMA_NOW.plus(3, ChronoUnit.DAYS)
        val card = libraryFlashcard(id = CARD_ID, word = "borrow", nextReviewAt = reviewAt.toEpochMilli())

        assertThat(card.scheduleStatus(now = LIMA_NOW, zone = LIMA_ZONE)).isEqualTo(ScheduleStatus.InDays(3))
    }

    private fun buildViewModel(
        libraryRepository: LibraryRepository = FakeLibraryRepository(),
        clock: Clock = FakeClock(FIXED_NOW),
    ): LibraryViewModel {
        val restoreFlashcardUseCase = mockk<RestoreFlashcardUseCase>()
        val getDecksUseCase = mockk<GetDecksUseCase>()
        every { getDecksUseCase() } returns flowOf(emptyList())

        return LibraryViewModel(
            searchLibrary = SearchLibraryUseCase(libraryRepository),
            restoreFlashcardUseCase = restoreFlashcardUseCase,
            undoEventHolder = UndoEventHolder(),
            getDecksUseCase = getDecksUseCase,
            clock = clock,
        )
    }

    private fun libraryFlashcard(
        id: FlashcardId,
        word: String,
        nextReviewAt: Long? = null,
    ): LibraryFlashcard = LibraryFlashcard(
        id = id,
        deckId = DECK_ID,
        deckName = "Primeras palabras",
        word = word,
        translation = "prestar",
        meaning = "",
        enrichmentStatus = EnrichmentStatus.ENRICHED,
        nextReviewAt = nextReviewAt,
    )

    private class FakeLibraryRepository(
        private val cards: MutableStateFlow<List<LibraryFlashcard>> = MutableStateFlow(emptyList()),
    ) : LibraryRepository {
        override fun observeLibrary(): Flow<List<LibraryFlashcard>> = cards

        fun emit(vararg flashcards: LibraryFlashcard) {
            cards.value = flashcards.toList()
        }
    }

    private class FakeClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private companion object {
        val DECK_ID: DeckId = "deck-1".toDeckId()
        val CARD_ID: FlashcardId = "card-1".toFlashcardId()
        val CARD_ID_2: FlashcardId = "card-2".toFlashcardId()
        val FIXED_NOW: Instant = Instant.parse("2026-08-28T12:00:00Z")
        val LIMA_ZONE: ZoneId = ZoneId.of("America/Lima")
        val LIMA_NOW: Instant = Instant.parse("2026-08-28T17:00:00Z")
        const val SEARCH_DEBOUNCE_MS: Long = 200L
    }
}

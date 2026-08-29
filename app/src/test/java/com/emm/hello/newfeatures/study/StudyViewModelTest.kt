package com.emm.hello.newfeatures.study

import app.cash.turbine.test
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.generation.EvaluationMode
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.StudyCardType
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.study.ReviewGrade
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import com.emm.domain.study.StudyFlashcard
import com.emm.domain.study.StudySessionRepository
import com.emm.domain.time.Clock
import com.emm.hello.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModelTest {

    private val fixedClock = Clock { Instant.parse("2026-05-04T12:30:45Z") }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init shows first flashcard and sets total count`() = runTest {
        val cards = listOf(studyFlashcard("a"), studyFlashcard("b"), studyFlashcard("c"))
        val viewModel = makeViewModel(cards)
        advanceUntilIdle()

        assertThat(viewModel.state.value.currentItem?.flashcardId?.value).isEqualTo("a")
        assertThat(viewModel.state.value.totalCount).isEqualTo(3)
    }

    @Test
    fun `total count is the number of flashcards even when one carries several study cards`() = runTest {
        val cards = listOf(
            studyFlashcard(
                id = "a",
                studyCards = listOf(
                    studyCard("a-1", StudyCardType.Recognition),
                    studyCard("a-2", StudyCardType.Production),
                    studyCard("a-3", StudyCardType.Cloze),
                ),
            ),
            studyFlashcard(id = "b", studyCards = emptyList()),
        )
        val viewModel = makeViewModel(cards)
        advanceUntilIdle()

        assertThat(viewModel.state.value.totalCount).isEqualTo(2)
    }

    @Test
    fun `exit clicked before any review emits navigate back`() = runTest {
        val viewModel = makeViewModel(listOf(studyFlashcard("a"), studyFlashcard("b")))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(StudyUiIntent.ExitClicked)
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.NavigateBack)
        }
    }

    @Test
    fun `create card clicked emits navigate to capture`() = runTest {
        val viewModel = makeViewModel(listOf(studyFlashcard("a")))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(StudyUiIntent.CreateCardClicked)
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.NavigateToCapture)
        }
    }

    @Test
    fun `get new words clicked emits navigate to suggest`() = runTest {
        val viewModel = makeViewModel(listOf(studyFlashcard("a")))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(StudyUiIntent.GetNewWordsClicked)
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.NavigateToSuggest)
        }
    }

    @Test
    fun `exit clicked mid-session emits navigate back without asking for confirmation`() = runTest {
        val viewModel = makeViewModel(listOf(studyFlashcard("a"), studyFlashcard("b")))
        advanceUntilIdle()
        viewModel.onIntent(
            StudyUiIntent.ReviewAnswered(
                item = viewModel.state.value.currentItem,
                reviewGrade = ReviewGrade.GOOD,
            )
        )
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(StudyUiIntent.ExitClicked)
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.NavigateBack)
        }
    }

    @Test
    fun `review answered advances to next card and increments reviewed count`() = runTest {
        val cards = listOf(studyFlashcard("a"), studyFlashcard("b"), studyFlashcard("c"))
        val viewModel = makeViewModel(cards)
        advanceUntilIdle()

        viewModel.onIntent(
            StudyUiIntent.ReviewAnswered(
                item = viewModel.state.value.currentItem,
                reviewGrade = ReviewGrade.GOOD,
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.currentItem?.flashcardId?.value).isEqualTo("b")
        assertThat(viewModel.state.value.reviewedCount).isEqualTo(1)
    }

    @Test
    fun `again counts as forgot and every other grade counts as knew`() = runTest {
        val cards = listOf(studyFlashcard("a"), studyFlashcard("b"), studyFlashcard("c"), studyFlashcard("d"))
        val viewModel = makeViewModel(cards)
        advanceUntilIdle()

        listOf(ReviewGrade.AGAIN, ReviewGrade.HARD, ReviewGrade.GOOD, ReviewGrade.EASY).forEach { grade ->
            viewModel.onIntent(
                StudyUiIntent.ReviewAnswered(
                    item = viewModel.state.value.currentItem,
                    reviewGrade = grade,
                )
            )
            advanceUntilIdle()
        }

        assertThat(viewModel.state.value.forgotCount).isEqualTo(1)
        assertThat(viewModel.state.value.knewCount).isEqualTo(3)
        assertThat(viewModel.state.value.reviewedCount).isEqualTo(4)
    }

    @Test
    fun `reloading the session resets the tallies`() = runTest {
        val viewModel = makeViewModel(listOf(studyFlashcard("a"), studyFlashcard("b")))
        advanceUntilIdle()
        viewModel.onIntent(
            StudyUiIntent.ReviewAnswered(
                item = viewModel.state.value.currentItem,
                reviewGrade = ReviewGrade.AGAIN,
            )
        )
        advanceUntilIdle()
        assertThat(viewModel.state.value.forgotCount).isEqualTo(1)

        viewModel.onIntent(StudyUiIntent.RetryLoad)
        advanceUntilIdle()

        assertThat(viewModel.state.value.forgotCount).isEqualTo(0)
        assertThat(viewModel.state.value.knewCount).isEqualTo(0)
        assertThat(viewModel.state.value.reviewedCount).isEqualTo(0)
    }

    @Test
    fun `each review is persisted immediately with the grade as given`() = runTest {
        val reviewRepo = FakeFlashcardReviewRepo()
        val viewModel = makeViewModel(
            listOf(studyFlashcard("a"), studyFlashcard("b")),
            reviewRepo = reviewRepo,
        )
        advanceUntilIdle()

        viewModel.onIntent(
            StudyUiIntent.ReviewAnswered(
                item = viewModel.state.value.currentItem,
                reviewGrade = ReviewGrade.AGAIN,
            )
        )
        advanceUntilIdle()
        assertThat(reviewRepo.updates.map { it.first.flashcardId.value to it.second })
            .containsExactly("a" to ReviewGrade.AGAIN)

        viewModel.onIntent(
            StudyUiIntent.ReviewAnswered(
                item = viewModel.state.value.currentItem,
                reviewGrade = ReviewGrade.GOOD,
            )
        )
        advanceUntilIdle()
        assertThat(reviewRepo.updates.map { it.first.flashcardId.value to it.second })
            .containsExactly("a" to ReviewGrade.AGAIN, "b" to ReviewGrade.GOOD)
            .inOrder()
    }

    @Test
    fun `flashcard with several study cards is graded once and keeps the grade as given`() = runTest {
        val reviewRepo = FakeFlashcardReviewRepo()
        val viewModel = makeViewModel(
            listOf(
                studyFlashcard(
                    id = "a",
                    studyCards = listOf(
                        studyCard("a-1", StudyCardType.Recognition),
                        studyCard("a-2", StudyCardType.Production),
                    ),
                )
            ),
            reviewRepo = reviewRepo,
        )
        advanceUntilIdle()

        viewModel.onIntent(
            StudyUiIntent.ReviewAnswered(
                item = viewModel.state.value.currentItem,
                reviewGrade = ReviewGrade.EASY,
            )
        )
        advanceUntilIdle()

        assertThat(reviewRepo.updates).hasSize(1)
        assertThat(reviewRepo.updates.single().first.flashcardId.value).isEqualTo("a")
        assertThat(reviewRepo.updates.single().second).isEqualTo(ReviewGrade.EASY)
        assertThat(viewModel.state.value.currentItem).isNull()
        assertThat(viewModel.state.value.reviewedCount).isEqualTo(1)
    }

    @Test
    fun `flashcard with no study cards persists the review once when graded`() = runTest {
        val reviewRepo = FakeFlashcardReviewRepo()
        val viewModel = makeViewModel(
            listOf(studyFlashcard(id = "a", studyCards = emptyList())),
            reviewRepo = reviewRepo,
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.totalCount).isEqualTo(1)

        viewModel.onIntent(
            StudyUiIntent.ReviewAnswered(
                item = viewModel.state.value.currentItem,
                reviewGrade = ReviewGrade.GOOD,
            )
        )
        advanceUntilIdle()

        assertThat(reviewRepo.updates).hasSize(1)
        assertThat(reviewRepo.updates.single().first.flashcardId.value).isEqualTo("a")
        assertThat(reviewRepo.updates.single().second).isEqualTo(ReviewGrade.GOOD)
    }

    @Test
    fun `review answered with no current item persists nothing`() = runTest {
        val reviewRepo = FakeFlashcardReviewRepo()
        val viewModel = makeViewModel(emptyList(), reviewRepo = reviewRepo)
        advanceUntilIdle()

        viewModel.onIntent(StudyUiIntent.ReviewAnswered(item = null, reviewGrade = ReviewGrade.GOOD))
        advanceUntilIdle()

        assertThat(reviewRepo.updates).isEmpty()
        assertThat(viewModel.state.value.reviewedCount).isEqualTo(0)
    }

    @Test
    fun `session finished is set only after the last card is graded`() = runTest {
        val cards = listOf(studyFlashcard("a"), studyFlashcard("b"))
        val viewModel = makeViewModel(cards)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(
                StudyUiIntent.ReviewAnswered(
                    item = viewModel.state.value.currentItem,
                    reviewGrade = ReviewGrade.GOOD,
                )
            )
            advanceUntilIdle()
            expectNoEvents()
            assertThat(viewModel.state.value.sessionFinished).isFalse()

            viewModel.onIntent(
                StudyUiIntent.ReviewAnswered(
                    item = viewModel.state.value.currentItem,
                    reviewGrade = ReviewGrade.GOOD,
                )
            )
            advanceUntilIdle()
            expectNoEvents()
        }
        assertThat(viewModel.state.value.sessionFinished).isTrue()
        assertThat(viewModel.state.value.currentItem).isNull()
    }

    @Test
    fun `session finished stays true and emits nothing when queue is already empty`() = runTest {
        val viewModel = makeViewModel(listOf(studyFlashcard("a")))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(
                StudyUiIntent.ReviewAnswered(
                    item = viewModel.state.value.currentItem,
                    reviewGrade = ReviewGrade.GOOD,
                )
            )
            advanceUntilIdle()
            expectNoEvents()
            assertThat(viewModel.state.value.sessionFinished).isTrue()

            viewModel.onIntent(
                StudyUiIntent.ReviewAnswered(
                    item = viewModel.state.value.currentItem,
                    reviewGrade = ReviewGrade.GOOD,
                )
            )
            advanceUntilIdle()
            expectNoEvents()
        }
        assertThat(viewModel.state.value.sessionFinished).isTrue()
    }

    @Test
    fun `empty session shows the empty state instead of finishing`() = runTest {
        val viewModel = makeViewModel(emptyList())

        viewModel.effect.test {
            advanceUntilIdle()
            expectNoEvents()
        }
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.currentItem).isNull()
        assertThat(viewModel.state.value.totalCount).isEqualTo(0)
        assertThat(viewModel.state.value.sessionFinished).isFalse()
    }

    @Test
    fun `non-sentinel deckId loads the per-deck session`() = runTest {
        val repo = FakeStudySessionRepo(listOf(studyFlashcard("a")))
        val viewModel = StudyViewModel(
            deckId = "deck-1",
            studySessionRepository = repo,
            scheduleFlashcardReviewUseCase = ScheduleFlashcardReviewUseCase(fixedClock),
            flashcardReviewRepository = FakeFlashcardReviewRepo(),
        )
        advanceUntilIdle()

        assertThat(repo.sessionTodayCalledWith?.value).isEqualTo("deck-1")
        assertThat(repo.sessionTodayAllDecksCalled).isFalse()
    }

    @Test
    fun `all-due-decks sentinel loads the all-decks session`() = runTest {
        val repo = FakeStudySessionRepo(
            studyFlashcards = emptyList(),
            allDecksFlashcards = listOf(studyFlashcard("a"), studyFlashcard("b")),
        )
        val viewModel = StudyViewModel(
            deckId = StudyRoute.ALL_DUE_DECKS,
            studySessionRepository = repo,
            scheduleFlashcardReviewUseCase = ScheduleFlashcardReviewUseCase(fixedClock),
            flashcardReviewRepository = FakeFlashcardReviewRepo(),
        )
        advanceUntilIdle()

        assertThat(repo.sessionTodayAllDecksCalled).isTrue()
        assertThat(repo.sessionTodayCalledWith).isNull()
        assertThat(viewModel.state.value.totalCount).isEqualTo(2)
    }

    @Test
    fun `successful load clears isLoading and leaves no error`() = runTest {
        val viewModel = makeViewModel(listOf(studyFlashcard("a")))
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.loadError).isNull()
    }

    @Test
    fun `failed load sets error and clears isLoading without showing empty`() = runTest {
        val repo = FakeStudySessionRepo(
            studyFlashcards = emptyList(),
            sessionTodayError = IllegalStateException("db read failed"),
        )
        val viewModel = StudyViewModel(
            deckId = "deck-1",
            studySessionRepository = repo,
            scheduleFlashcardReviewUseCase = ScheduleFlashcardReviewUseCase(fixedClock),
            flashcardReviewRepository = FakeFlashcardReviewRepo(),
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.loadError).isEqualTo(StudyLoadError.SessionLoadFailed)
        assertThat(viewModel.state.value.totalCount).isEqualTo(0)
    }

    @Test
    fun `retry after failure reloads and clears the error`() = runTest {
        val repo = RecoveringStudySessionRepo(
            recoveredFlashcards = listOf(studyFlashcard("a")),
        )
        val viewModel = StudyViewModel(
            deckId = "deck-1",
            studySessionRepository = repo,
            scheduleFlashcardReviewUseCase = ScheduleFlashcardReviewUseCase(fixedClock),
            flashcardReviewRepository = FakeFlashcardReviewRepo(),
        )
        advanceUntilIdle()
        assertThat(viewModel.state.value.loadError).isEqualTo(StudyLoadError.SessionLoadFailed)

        viewModel.onIntent(StudyUiIntent.RetryLoad)
        advanceUntilIdle()

        assertThat(viewModel.state.value.loadError).isNull()
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.totalCount).isEqualTo(1)
    }

    private fun makeViewModel(
        cards: List<StudyFlashcard>,
        reviewRepo: FlashcardReviewRepository = FakeFlashcardReviewRepo(),
    ): StudyViewModel = StudyViewModel(
        deckId = "deck-1",
        studySessionRepository = FakeStudySessionRepo(cards),
        scheduleFlashcardReviewUseCase = ScheduleFlashcardReviewUseCase(fixedClock),
        flashcardReviewRepository = reviewRepo,
    )

    private fun studyFlashcard(
        id: String,
        studyCards: List<GeneratedStudyCard> = listOf(
            studyCard("$id-rec", StudyCardType.Recognition)
        ),
    ): StudyFlashcard = StudyFlashcard(
        flashcardId = id.toFlashcardId(),
        word = id,
        phonetic = "",
        meaning = "",
        translation = "",
        review = FsrsCard.new(id.toFlashcardId(), fixedClock),
        studyCards = studyCards,
    )

    private fun studyCard(id: String, type: StudyCardType) = GeneratedStudyCard(
        cardId = id,
        cardType = type,
        prompt = id,
        expectedAnswer = id,
        evaluationMode = EvaluationMode.ManualSelfCheck,
    )

    private class FakeStudySessionRepo(
        private val studyFlashcards: List<StudyFlashcard>,
        private val allDecksFlashcards: List<StudyFlashcard> = studyFlashcards,
        private val sessionTodayError: Throwable? = null,
        private val allDecksError: Throwable? = null,
    ) : StudySessionRepository {
        var sessionTodayCalledWith: DeckId? = null
        var sessionTodayAllDecksCalled: Boolean = false

        override suspend fun sessionToday(deckId: DeckId): List<StudyFlashcard> {
            sessionTodayCalledWith = deckId
            sessionTodayError?.let { throw it }
            return studyFlashcards
        }

        override suspend fun sessionTodayAllDecks(): List<StudyFlashcard> {
            sessionTodayAllDecksCalled = true
            allDecksError?.let { throw it }
            return allDecksFlashcards
        }

        override fun flashcardWithReview(deckId: DeckId): Flow<List<StudyFlashcard>> = emptyFlow()
    }

    private class RecoveringStudySessionRepo(
        private val recoveredFlashcards: List<StudyFlashcard>,
    ) : StudySessionRepository {
        private var calls = 0

        override suspend fun sessionToday(deckId: DeckId): List<StudyFlashcard> {
            calls += 1
            if (calls == 1) error("db read failed")
            return recoveredFlashcards
        }

        override suspend fun sessionTodayAllDecks(): List<StudyFlashcard> = recoveredFlashcards
        override fun flashcardWithReview(deckId: DeckId): Flow<List<StudyFlashcard>> = emptyFlow()
    }

    private class FakeFlashcardReviewRepo : FlashcardReviewRepository {
        val updates = mutableListOf<Pair<FsrsCard, ReviewGrade>>()
        override fun all(): Flow<List<FsrsCard>> = emptyFlow()
        override suspend fun update(card: FsrsCard, grade: ReviewGrade) {
            updates += card to grade
        }
    }
}

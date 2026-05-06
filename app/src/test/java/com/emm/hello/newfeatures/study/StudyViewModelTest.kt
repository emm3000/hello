package com.emm.hello.newfeatures.study

import app.cash.turbine.test
import com.emm.domain.flashcard.EvaluationMode
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.flashcard.GeneratedStudyCard
import com.emm.domain.flashcard.StudyCardType
import com.emm.domain.flashcard.UpdateFlashcardReviewUseCase
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.study.GetStudySessionUseCase
import com.emm.domain.study.ReviewGrade
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import com.emm.domain.study.StudySessionRepository
import com.emm.domain.time.Clock
import com.emm.domain.ids.DeckId
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
        val cards = listOf(flashcard("a"), flashcard("b"), flashcard("c"))
        val viewModel = makeViewModel(cards)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.currentItem?.flashcard?.id?.value).isEqualTo("a")
        assertThat(viewModel.uiState.value.totalCount).isEqualTo(3)
    }

    @Test
    fun `back clicked emits navigate back effect`() = runTest {
        val viewModel = makeViewModel(listOf(flashcard("a"), flashcard("b")))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(StudyUiIntent.BackClicked)
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.NavigateBack)
        }
    }

    @Test
    fun `finish dialog dismissed emits navigate back effect`() = runTest {
        val viewModel = makeViewModel(listOf(flashcard("a"), flashcard("b")))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(StudyUiIntent.FinishDialogDismissed)
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.NavigateBack)
        }
    }

    @Test
    fun `review answered advances to next card and increments reviewed count`() = runTest {
        val cards = listOf(flashcard("a"), flashcard("b"), flashcard("c"))
        val viewModel = makeViewModel(cards)
        advanceUntilIdle()

        viewModel.onIntent(
            StudyUiIntent.ReviewAnswered(
                item = viewModel.uiState.value.currentItem,
                reviewGrade = ReviewGrade.GOOD,
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.currentItem?.flashcard?.id?.value).isEqualTo("b")
        assertThat(viewModel.uiState.value.reviewedCount).isEqualTo(1)
    }

    @Test
    fun `reviewing last card emits session finished effect`() = runTest {
        val cards = listOf(flashcard("a"), flashcard("b"))
        val viewModel = makeViewModel(cards)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(
                StudyUiIntent.ReviewAnswered(
                    item = viewModel.uiState.value.currentItem,
                    reviewGrade = ReviewGrade.GOOD,
                )
            )
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.SessionFinished)
        }
    }

    @Test
    fun `session finished is not emitted twice when queue is empty`() = runTest {
        val cards = listOf(flashcard("a"), flashcard("b"))
        val viewModel = makeViewModel(cards)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(
                StudyUiIntent.ReviewAnswered(
                    item = viewModel.uiState.value.currentItem,
                    reviewGrade = ReviewGrade.GOOD,
                )
            )
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.SessionFinished)

            viewModel.onIntent(
                StudyUiIntent.ReviewAnswered(
                    item = viewModel.uiState.value.currentItem,
                    reviewGrade = ReviewGrade.GOOD,
                )
            )
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `empty session emits session finished immediately`() = runTest {
        val viewModel = makeViewModel(emptyList())

        viewModel.effect.test {
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.SessionFinished)
        }
    }

    @Test
    fun `flashcard with multiple study cards expands session and schedules once`() = runTest {
        val reviewRepo = FakeFlashcardReviewRepo()
        val viewModel = StudyViewModel(
            deckId = "deck-1",
            getStudySessionUseCase = GetStudySessionUseCase(
                FakeStudySessionRepo(
                    listOf(
                        flashcard(
                            id = "a",
                            studyCards = listOf(
                                studyCard("a-1", StudyCardType.Recognition),
                                studyCard("a-2", StudyCardType.Production),
                            ),
                        )
                    )
                )
            ),
            scheduleFlashcardReviewUseCase = ScheduleFlashcardReviewUseCase(fixedClock),
            updateFlashcardReviewUseCase = UpdateFlashcardReviewUseCase(reviewRepo),
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.totalCount).isEqualTo(2)
        assertThat(viewModel.uiState.value.currentItem?.studyCard?.cardId).isEqualTo("a-1")

        viewModel.onIntent(
            StudyUiIntent.ReviewAnswered(
                item = viewModel.uiState.value.currentItem,
                reviewGrade = ReviewGrade.EASY,
            )
        )
        advanceUntilIdle()

        assertThat(reviewRepo.updates).isEmpty()
        assertThat(viewModel.uiState.value.currentItem?.studyCard?.cardId).isEqualTo("a-2")

        viewModel.onIntent(
            StudyUiIntent.ReviewAnswered(
                item = viewModel.uiState.value.currentItem,
                reviewGrade = ReviewGrade.HARD,
            )
        )
        advanceUntilIdle()

        assertThat(reviewRepo.updates).hasSize(1)
        assertThat(reviewRepo.updates.single().flashcardId.value).isEqualTo("a")
    }

    private fun makeViewModel(cards: List<Flashcard>): StudyViewModel = StudyViewModel(
        deckId = "deck-1",
        getStudySessionUseCase = GetStudySessionUseCase(FakeStudySessionRepo(cards)),
        scheduleFlashcardReviewUseCase = ScheduleFlashcardReviewUseCase(fixedClock),
        updateFlashcardReviewUseCase = UpdateFlashcardReviewUseCase(FakeFlashcardReviewRepo()),
    )

    private fun flashcard(
        id: String,
        studyCards: List<GeneratedStudyCard> = listOf(
            studyCard("$id-rec", StudyCardType.Recognition)
        ),
    ): Flashcard = Flashcard(
        id = id.toFlashcardId(),
        word = id,
        meaning = "",
        translation = "",
        examples = emptyList(),
        phonetic = "",
        review = FlashcardReview.empty(fixedClock),
        studyCards = studyCards,
    )

    private fun studyCard(id: String, type: StudyCardType) = GeneratedStudyCard(
        cardId = id,
        cardType = type,
        prompt = id,
        expectedAnswer = id,
        evaluationMode = EvaluationMode.ManualSelfCheck,
    )

    private class FakeStudySessionRepo(private val flashcards: List<Flashcard>) : StudySessionRepository {
        override suspend fun sessionToday(deckId: DeckId): List<Flashcard> = flashcards
        override fun flashcardWithReview(deckId: DeckId): Flow<List<Flashcard>> = emptyFlow()
    }

    private class FakeFlashcardReviewRepo : FlashcardReviewRepository {
        val updates = mutableListOf<FlashcardReview>()
        override fun all(): Flow<List<FlashcardReview>> = emptyFlow()
        override suspend fun update(flashcardReview: FlashcardReview) {
            updates += flashcardReview
        }
    }
}

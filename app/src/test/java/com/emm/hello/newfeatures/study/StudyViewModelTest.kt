package com.emm.hello.newfeatures.study

import app.cash.turbine.test
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.flashcard.GetStudySessionUseCase
import com.emm.domain.flashcard.StudySessionRepository
import com.emm.domain.flashcard.UpdateFlashcardReviewUseCase
import com.emm.domain.study.ReviewGrade
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import com.emm.hello.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init shows first flashcard and sets total count`() = runTest {
        val cards = listOf(flashcard("a"), flashcard("b"), flashcard("c"))
        val viewModel = makeViewModel(cards)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.currentFlashcard?.id).isEqualTo("a")
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

        viewModel.onIntent(StudyUiIntent.ReviewAnswered(flashcard("a"), ReviewGrade.GOOD))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.currentFlashcard?.id).isEqualTo("b")
        assertThat(viewModel.uiState.value.reviewedCount).isEqualTo(1)
    }

    @Test
    fun `reviewing last card emits session finished effect`() = runTest {
        val cards = listOf(flashcard("a"), flashcard("b"))
        val viewModel = makeViewModel(cards)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(StudyUiIntent.ReviewAnswered(flashcard("a"), ReviewGrade.GOOD))
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.SessionFinished)
        }
    }

    @Test
    fun `session finished is not emitted twice when queue is empty`() = runTest {
        val cards = listOf(flashcard("a"), flashcard("b"))
        val viewModel = makeViewModel(cards)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(StudyUiIntent.ReviewAnswered(flashcard("a"), ReviewGrade.GOOD))
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.SessionFinished)

            viewModel.onIntent(StudyUiIntent.ReviewAnswered(flashcard("b"), ReviewGrade.GOOD))
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

    private fun makeViewModel(cards: List<Flashcard>): StudyViewModel = StudyViewModel(
        deckId = "deck-1",
        getStudySessionUseCase = GetStudySessionUseCase(FakeStudySessionRepo(cards)),
        scheduleFlashcardReviewUseCase = ScheduleFlashcardReviewUseCase(),
        updateFlashcardReviewUseCase = UpdateFlashcardReviewUseCase(FakeFlashcardReviewRepo()),
    )

    private fun flashcard(id: String): Flashcard = Flashcard(
        id = id,
        word = id,
        meaning = "",
        translation = "",
        examples = emptyList(),
        phonetic = "",
        review = FlashcardReview.Empty,
    )

    private class FakeStudySessionRepo(private val flashcards: List<Flashcard>) : StudySessionRepository {
        override suspend fun sessionToday(deckId: String): List<Flashcard> = flashcards
        override fun flashcardWithReview(deckId: String): Flow<List<Flashcard>> = emptyFlow()
    }

    private class FakeFlashcardReviewRepo : FlashcardReviewRepository {
        val updates = mutableListOf<FlashcardReview>()
        override fun all(): Flow<List<FlashcardReview>> = emptyFlow()
        override suspend fun update(flashcardReview: FlashcardReview) {
            updates += flashcardReview
        }
    }
}

package com.emm.hello.newfeatures.study

import app.cash.turbine.test
import com.emm.domain.generation.EvaluationMode
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.StudyCardType
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.domain.study.ReviewGrade
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import com.emm.domain.study.StudyFlashcard
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
        val cards = listOf(studyFlashcard("a"), studyFlashcard("b"), studyFlashcard("c"))
        val viewModel = makeViewModel(cards)
        advanceUntilIdle()

        assertThat(viewModel.state.value.currentItem?.flashcardId?.value).isEqualTo("a")
        assertThat(viewModel.state.value.totalCount).isEqualTo(3)
    }

    @Test
    fun `back clicked emits navigate back effect`() = runTest {
        val viewModel = makeViewModel(listOf(studyFlashcard("a"), studyFlashcard("b")))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(StudyUiIntent.BackClicked)
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.NavigateBack)
        }
    }

    @Test
    fun `finish dialog dismissed emits navigate back effect`() = runTest {
        val viewModel = makeViewModel(listOf(studyFlashcard("a"), studyFlashcard("b")))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(StudyUiIntent.FinishDialogDismissed)
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
    fun `reviewing last card emits session finished effect`() = runTest {
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
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.SessionFinished)
        }
    }

    @Test
    fun `session finished is not emitted twice when queue is empty`() = runTest {
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
            assertThat(awaitItem()).isEqualTo(StudyUiEffect.SessionFinished)

            viewModel.onIntent(
                StudyUiIntent.ReviewAnswered(
                    item = viewModel.state.value.currentItem,
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
            studySessionRepository = FakeStudySessionRepo(
                listOf(
                    studyFlashcard(
                        id = "a",
                        studyCards = listOf(
                            studyCard("a-1", StudyCardType.Recognition),
                            studyCard("a-2", StudyCardType.Production),
                        ),
                    )
                )
            ),
            scheduleFlashcardReviewUseCase = ScheduleFlashcardReviewUseCase(fixedClock),
            flashcardReviewRepository = reviewRepo,
            clock = fixedClock,
            onboardingState = FakeOnboardingStateRepository(),
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.totalCount).isEqualTo(2)
        assertThat(viewModel.state.value.currentItem?.studyCard?.cardId).isEqualTo("a-1")

        viewModel.onIntent(
            StudyUiIntent.ReviewAnswered(
                item = viewModel.state.value.currentItem,
                reviewGrade = ReviewGrade.EASY,
            )
        )
        advanceUntilIdle()

        assertThat(reviewRepo.updates).isEmpty()
        assertThat(viewModel.state.value.currentItem?.studyCard?.cardId).isEqualTo("a-2")

        viewModel.onIntent(
            StudyUiIntent.ReviewAnswered(
                item = viewModel.state.value.currentItem,
                reviewGrade = ReviewGrade.HARD,
            )
        )
        advanceUntilIdle()

        assertThat(reviewRepo.updates).hasSize(1)
        assertThat(reviewRepo.updates.single().flashcardId.value).isEqualTo("a")
    }

    // ── Grade hint ───────────────────────────────────────────────────────────

    @Test
    fun `grade hint shows when flag is false and session has cards`() = runTest {
        val repo = FakeOnboardingStateRepository(gradeHintSeen = false)
        val viewModel = makeViewModel(listOf(studyFlashcard("a")), onboardingRepo = repo)
        advanceUntilIdle()

        assertThat(viewModel.state.value.isGradeHintVisible).isTrue()
    }

    @Test
    fun `grade hint does not show when flag is already true`() = runTest {
        val repo = FakeOnboardingStateRepository(gradeHintSeen = true)
        val viewModel = makeViewModel(listOf(studyFlashcard("a")), onboardingRepo = repo)
        advanceUntilIdle()

        assertThat(viewModel.state.value.isGradeHintVisible).isFalse()
    }

    @Test
    fun `GradeHintDismissed marks hint seen and hides it`() = runTest {
        val repo = FakeOnboardingStateRepository(gradeHintSeen = false)
        val viewModel = makeViewModel(listOf(studyFlashcard("a")), onboardingRepo = repo)
        advanceUntilIdle()

        viewModel.onIntent(StudyUiIntent.GradeHintDismissed)

        assertThat(viewModel.state.value.isGradeHintVisible).isFalse()
        assertThat(repo.gradeHintSeenCalled).isTrue()
    }

    @Test
    fun `first review answer marks grade hint seen and hides it`() = runTest {
        val repo = FakeOnboardingStateRepository(gradeHintSeen = false)
        val viewModel = makeViewModel(listOf(studyFlashcard("a"), studyFlashcard("b")), onboardingRepo = repo)
        advanceUntilIdle()

        viewModel.onIntent(
            StudyUiIntent.ReviewAnswered(
                item = viewModel.state.value.currentItem,
                reviewGrade = ReviewGrade.GOOD,
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.isGradeHintVisible).isFalse()
        assertThat(repo.gradeHintSeenCalled).isTrue()
    }

    @Test
    fun `grade hint does not show for empty session`() = runTest {
        val repo = FakeOnboardingStateRepository(gradeHintSeen = false)
        val viewModel = makeViewModel(emptyList(), onboardingRepo = repo)
        advanceUntilIdle()

        assertThat(viewModel.state.value.isGradeHintVisible).isFalse()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makeViewModel(
        cards: List<StudyFlashcard>,
        onboardingRepo: OnboardingStateRepository = FakeOnboardingStateRepository(),
    ): StudyViewModel = StudyViewModel(
        deckId = "deck-1",
        studySessionRepository = FakeStudySessionRepo(cards),
        scheduleFlashcardReviewUseCase = ScheduleFlashcardReviewUseCase(fixedClock),
        flashcardReviewRepository = FakeFlashcardReviewRepo(),
        clock = fixedClock,
        onboardingState = onboardingRepo,
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

    private class FakeStudySessionRepo(private val studyFlashcards: List<StudyFlashcard>) : StudySessionRepository {
        override suspend fun sessionToday(deckId: DeckId): List<StudyFlashcard> = studyFlashcards
        override fun flashcardWithReview(deckId: DeckId): Flow<List<StudyFlashcard>> = emptyFlow()
    }

    private class FakeFlashcardReviewRepo : FlashcardReviewRepository {
        val updates = mutableListOf<FlashcardReview>()
        override fun all(): Flow<List<FlashcardReview>> = emptyFlow()
        override suspend fun update(flashcardReview: FlashcardReview) {
            updates += flashcardReview
        }
    }
}

private class FakeOnboardingStateRepository(
    private var gradeHintSeen: Boolean = false,
) : OnboardingStateRepository {
    var gradeHintSeenCalled: Boolean = false

    override fun hasSeenWelcome(): Boolean = true
    override fun markWelcomeSeen() = Unit
    override fun hasSeenGradeHint(): Boolean = gradeHintSeen
    override fun markGradeHintSeen() {
        gradeHintSeenCalled = true
        gradeHintSeen = true
    }
}

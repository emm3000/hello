package com.emm.hello.newfeatures.study

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.ids.toDeckId
import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.domain.study.ReviewGrade
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import com.emm.domain.study.StudyFlashcard
import com.emm.domain.study.StudySessionRepository
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class StudyViewModel(
    deckId: String,
    private val studySessionRepository: StudySessionRepository,
    private val scheduleFlashcardReviewUseCase: ScheduleFlashcardReviewUseCase,
    private val flashcardReviewRepository: FlashcardReviewRepository,
    private val onboardingState: OnboardingStateRepository,
) : MviViewModel<StudyUiState, StudyUiIntent, StudyUiEffect>(
    initialState = StudyUiState(),
) {

    // Null target == study all cards due today across decks (global Dashboard CTA).
    // Non-null == single deck (per-deck DeckDetail CTA). See StudyRoute.ALL_DUE_DECKS.
    private val deckId: String? = deckId.takeUnless { it == StudyRoute.ALL_DUE_DECKS }

    // One entry per due flashcard. Each entry is graded exactly once and persisted on the spot.
    private val studyItemsForToday: ArrayDeque<StudySessionItem> = ArrayDeque()

    init {
        loadSession()
    }

    private fun loadSession() = viewModelScope.launch {
        studyItemsForToday.clear()
        setState { copy(isLoading = true, loadError = null, reviewedCount = 0, sessionFinished = false) }
        try {
            val items = fetchSession().map { it.toStudySessionItem() }
            studyItemsForToday.addAll(items)
            val showHint = items.isNotEmpty() && !onboardingState.hasSeenGradeHint()
            setState { copy(isLoading = false, totalCount = items.size, isGradeHintVisible = showHint) }
            showNextCard()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "loadSession:error ${e.message}", e)
            setState { copy(isLoading = false, loadError = StudyLoadError.SessionLoadFailed) }
        }
    }

    private suspend fun fetchSession(): List<StudyFlashcard> {
        val target = deckId
        return if (target == null) {
            studySessionRepository.sessionTodayAllDecks()
        } else {
            studySessionRepository.sessionToday(target.toDeckId())
        }
    }

    private fun showNextCard() {
        val nextItem = studyItemsForToday.removeFirstOrNull()
        setState { copy(currentItem = nextItem) }
        // The session is over once the last card has been graded, i.e. there is nothing left to
        // show. An empty session never "finishes": it renders the empty state instead.
        val state = currentState
        if (nextItem == null && state.totalCount > 0 && !state.sessionFinished) {
            setState { copy(sessionFinished = true) }
            sendEffect(StudyUiEffect.SessionFinished)
        }
    }

    override fun onIntent(intent: StudyUiIntent) {
        when (intent) {
            StudyUiIntent.FinishDialogDismissed -> sendEffect(StudyUiEffect.NavigateBack)
            StudyUiIntent.CreateCardClicked -> sendEffect(StudyUiEffect.NavigateToNewCard)
            StudyUiIntent.GradeHintDismissed -> dismissGradeHint()
            StudyUiIntent.RetryLoad -> loadSession()
            StudyUiIntent.ExitClicked -> sendEffect(StudyUiEffect.NavigateBack)
            is StudyUiIntent.ReviewAnswered -> processReviewAnswer(
                item = intent.item,
                grade = intent.reviewGrade,
            )
        }
    }

    private fun dismissGradeHint() {
        if (!currentState.isGradeHintVisible) return
        onboardingState.markGradeHintSeen()
        setState { copy(isGradeHintVisible = false) }
    }

    private fun processReviewAnswer(item: StudySessionItem?, grade: ReviewGrade) = viewModelScope.launch {
        if (currentState.isGradeHintVisible) {
            dismissGradeHint()
        }
        val reviewedItem = item ?: return@launch
        val newCard: FsrsCard = scheduleFlashcardReviewUseCase(
            card = reviewedItem.review,
            grade = grade,
            flashcardId = reviewedItem.flashcardId,
        )
        flashcardReviewRepository.update(newCard, grade)
        setState { copy(reviewedCount = reviewedCount + 1) }
        showNextCard()
    }
}

private const val TAG = "StudyViewModel"

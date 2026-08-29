package com.emm.hello.newfeatures.study

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.ids.toDeckId
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
) : MviViewModel<StudyUiState, StudyUiIntent, StudyUiEffect>(
    initialState = StudyUiState(),
) {

    private val deckId: String? = deckId.takeUnless { it == StudyRoute.ALL_DUE_DECKS }

    private val studyItemsForToday: ArrayDeque<StudySessionItem> = ArrayDeque()

    init {
        loadSession()
    }

    private fun loadSession() = viewModelScope.launch {
        studyItemsForToday.clear()
        setState {
            copy(
                isLoading = true,
                loadError = null,
                reviewedCount = 0,
                knewCount = 0,
                forgotCount = 0,
                sessionFinished = false,
            )
        }
        try {
            val items = fetchSession().map { it.toStudySessionItem() }
            studyItemsForToday.addAll(items)
            setState { copy(isLoading = false, totalCount = items.size) }
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
        val state = currentState
        if (nextItem == null && state.totalCount > 0 && !state.sessionFinished) {
            setState { copy(sessionFinished = true) }
        }
    }

    override fun onIntent(intent: StudyUiIntent) {
        when (intent) {
            StudyUiIntent.CreateCardClicked -> sendEffect(StudyUiEffect.NavigateToCapture)
            StudyUiIntent.GetNewWordsClicked -> sendEffect(StudyUiEffect.NavigateToSuggest)
            StudyUiIntent.RetryLoad -> loadSession()
            StudyUiIntent.ExitClicked -> sendEffect(StudyUiEffect.NavigateBack)
            is StudyUiIntent.ReviewAnswered -> processReviewAnswer(
                item = intent.item,
                grade = intent.reviewGrade,
            )
        }
    }

    private fun processReviewAnswer(item: StudySessionItem?, grade: ReviewGrade) = viewModelScope.launch {
        val reviewedItem = item ?: return@launch
        val newCard: FsrsCard = scheduleFlashcardReviewUseCase(
            card = reviewedItem.review,
            grade = grade,
            flashcardId = reviewedItem.flashcardId,
        )
        flashcardReviewRepository.update(newCard, grade)
        setState { tallied(grade) }
        showNextCard()
    }
}

private fun StudyUiState.tallied(grade: ReviewGrade): StudyUiState = when (grade) {
    ReviewGrade.AGAIN -> copy(reviewedCount = reviewedCount + 1, forgotCount = forgotCount + 1)
    ReviewGrade.HARD,
    ReviewGrade.GOOD,
    ReviewGrade.EASY -> copy(reviewedCount = reviewedCount + 1, knewCount = knewCount + 1)
}

private const val TAG = "StudyViewModel"

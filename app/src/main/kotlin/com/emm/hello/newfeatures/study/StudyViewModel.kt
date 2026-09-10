package com.emm.hello.newfeatures.study

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.ids.toDeckId
import com.emm.domain.study.EXTRA_NEW_CARDS_PER_REQUEST
import com.emm.domain.study.GetStudySessionUseCase
import com.emm.domain.study.ReviewGrade
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import com.emm.domain.study.StudySession
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class StudyViewModel(
    deckId: String,
    extraNewCards: Int,
    private val getStudySessionUseCase: GetStudySessionUseCase,
    private val scheduleFlashcardReviewUseCase: ScheduleFlashcardReviewUseCase,
    private val flashcardReviewRepository: FlashcardReviewRepository,
) : MviViewModel<StudyUiState, StudyUiIntent, StudyUiEffect>(
    initialState = StudyUiState(),
) {

    private val deckId: String? = deckId.takeUnless { it == StudyRoute.ALL_DUE_DECKS }

    private val initialExtraNewCards: Int = extraNewCards

    private val studyItemsForToday: ArrayDeque<StudySessionItem> = ArrayDeque()

    init {
        loadSession(initialExtraNewCards)
    }

    private fun loadSession(extraNewCards: Int = 0) = viewModelScope.launch {
        studyItemsForToday.clear()
        setState {
            copy(
                isLoading = true,
                loadError = null,
                reviewedCount = 0,
                knewCount = 0,
                forgotCount = 0,
                sessionFinished = false,
                moreNewCards = 0,
            )
        }
        try {
            val session: StudySession = fetchSession(extraNewCards)
            val items: List<StudySessionItem> = session.cards.map { it.toStudySessionItem() }
            studyItemsForToday.addAll(items)
            setState {
                copy(
                    isLoading = false,
                    totalCount = items.size,
                    moreNewCards = minOf(session.heldBackNewCards, EXTRA_NEW_CARDS_PER_REQUEST),
                )
            }
            showNextCard()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "loadSession:error ${e.message}", e)
            setState { copy(isLoading = false, loadError = StudyLoadError.SessionLoadFailed) }
        }
    }

    private suspend fun fetchSession(extraNewCards: Int): StudySession =
        getStudySessionUseCase(deckId?.toDeckId(), extraNewCards)

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
            StudyUiIntent.StudyMoreClicked -> loadSession(EXTRA_NEW_CARDS_PER_REQUEST)
            StudyUiIntent.RetryLoad -> loadSession(initialExtraNewCards)
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

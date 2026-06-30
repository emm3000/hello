package com.emm.hello.newfeatures.study

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsParameters
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toDeckId
import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.domain.study.PreviewNextInterval
import com.emm.domain.study.ReviewGrade
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import com.emm.domain.study.StudyFlashcard
import com.emm.domain.study.StudySessionRepository
import com.emm.domain.time.Clock
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class StudyViewModel(
    deckId: String,
    private val studySessionRepository: StudySessionRepository,
    private val scheduleFlashcardReviewUseCase: ScheduleFlashcardReviewUseCase,
    private val flashcardReviewRepository: FlashcardReviewRepository,
    private val clock: Clock,
    private val onboardingState: OnboardingStateRepository,
    private val fsrsParameters: FsrsParameters = FsrsParameters.DEFAULT,
) : MviViewModel<StudyUiState, StudyUiIntent, StudyUiEffect>(
    initialState = StudyUiState(),
) {

    // Null target == study all cards due today across decks (global Dashboard CTA).
    // Non-null == single deck (per-deck DeckDetail CTA). See StudyRoute.ALL_DUE_DECKS.
    private val deckId: String? = deckId.takeUnless { it == StudyRoute.ALL_DUE_DECKS }

    private val studyItemsForToday: ArrayDeque<StudySessionItem> = ArrayDeque()
    private val pendingItemsByFlashcardId = mutableMapOf<FlashcardId, Int>()
    private val aggregatedGradesByFlashcardId = mutableMapOf<FlashcardId, ReviewGrade>()
    private val cardsByFlashcardId = mutableMapOf<FlashcardId, FsrsCard>()

    init {
        loadSession()
    }

    private fun loadSession() = viewModelScope.launch {
        resetSessionAccumulators()
        setState { copy(isLoading = true, loadError = null, reviewedCount = 0, sessionFinished = false) }
        try {
            val studyFlashcards: List<StudyFlashcard> = fetchSession()
            val items = studyFlashcards.flatMap { sf ->
                cardsByFlashcardId[sf.flashcardId] = sf.review
                pendingItemsByFlashcardId[sf.flashcardId] = sf.studyCards.count { it.isActive }
                sf.toStudySessionItems()
            }
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

    private fun resetSessionAccumulators() {
        studyItemsForToday.clear()
        pendingItemsByFlashcardId.clear()
        aggregatedGradesByFlashcardId.clear()
        cardsByFlashcardId.clear()
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
        setState {
            val nextItem = studyItemsForToday.removeFirstOrNull()
            val previews = nextItem?.let { item ->
                val liveCard = cardsByFlashcardId[item.flashcardId] ?: item.review
                PreviewNextInterval.previewAll(liveCard, clock, fsrsParameters)
            } ?: emptyMap()
            copy(currentItem = nextItem, intervalPreviews = previews)
        }
        if (studyItemsForToday.isEmpty() && !currentState.sessionFinished) {
            setState { copy(sessionFinished = true) }
            sendEffect(StudyUiEffect.SessionFinished)
        }
    }

    override fun onIntent(intent: StudyUiIntent) {
        when (intent) {
            StudyUiIntent.FinishDialogDismissed -> sendEffect(StudyUiEffect.NavigateBack)
            StudyUiIntent.CreateCardClicked -> sendEffect(StudyUiEffect.NavigateToNewCard)
            StudyUiIntent.GradeHintDismissed -> dismissGradeHint()
            StudyUiIntent.StartSession -> setState { copy(sessionStarted = true) }
            StudyUiIntent.RetryLoad -> loadSession()
            StudyUiIntent.RequestExit -> requestExit()
            StudyUiIntent.ConfirmExit -> {
                setState { copy(showExitConfirmation = false) }
                sendEffect(StudyUiEffect.NavigateBack)
            }
            StudyUiIntent.DismissExitConfirmation -> setState { copy(showExitConfirmation = false) }
            is StudyUiIntent.ReviewAnswered -> processReviewAnswer(
                item = intent.item,
                reviewResult = intent.reviewGrade,
            )
        }
    }

    private fun requestExit() {
        val state = currentState
        if (state.reviewedCount > 0 || state.sessionStarted) {
            setState { copy(showExitConfirmation = true) }
        } else {
            sendEffect(StudyUiEffect.NavigateBack)
        }
    }

    private fun dismissGradeHint() {
        if (!currentState.isGradeHintVisible) return
        onboardingState.markGradeHintSeen()
        setState { copy(isGradeHintVisible = false) }
    }

    private fun processReviewAnswer(item: StudySessionItem?, reviewResult: ReviewGrade) = viewModelScope.launch {
        if (currentState.isGradeHintVisible) {
            dismissGradeHint()
        }
        val flashcardId = item?.flashcardId ?: return@launch
        aggregatedGradesByFlashcardId[flashcardId] = moreConservativeGrade(
            current = aggregatedGradesByFlashcardId[flashcardId],
            incoming = reviewResult,
        )

        val remainingItems = pendingItemsByFlashcardId.getValue(flashcardId) - 1
        pendingItemsByFlashcardId[flashcardId] = remainingItems

        if (remainingItems == 0) {
            val finalGrade = aggregatedGradesByFlashcardId.remove(flashcardId) ?: reviewResult
            val persistedCard = cardsByFlashcardId.getValue(flashcardId)
            val newCard: FsrsCard = scheduleFlashcardReviewUseCase(
                card = persistedCard,
                grade = finalGrade,
                flashcardId = flashcardId,
            )
            flashcardReviewRepository.update(newCard, finalGrade)
            pendingItemsByFlashcardId.remove(flashcardId)
            cardsByFlashcardId.remove(flashcardId)
        }

        setState { copy(reviewedCount = reviewedCount + 1) }
        showNextCard()
    }

    private fun moreConservativeGrade(current: ReviewGrade?, incoming: ReviewGrade): ReviewGrade {
        val currentScore = current?.priority ?: Int.MAX_VALUE
        return if (incoming.priority < currentScore) incoming else current ?: incoming
    }
}

private val ReviewGrade.priority: Int
    get() = when (this) {
        ReviewGrade.AGAIN -> 0
        ReviewGrade.HARD -> 1
        ReviewGrade.GOOD -> 2
        ReviewGrade.EASY -> 3
    }

private const val TAG = "StudyViewModel"

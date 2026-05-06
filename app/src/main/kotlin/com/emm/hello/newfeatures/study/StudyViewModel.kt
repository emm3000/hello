package com.emm.hello.newfeatures.study

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toDeckId
import com.emm.domain.study.ReviewGrade
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import com.emm.domain.study.StudyFlashcard
import com.emm.domain.study.StudySessionRepository
import com.emm.hello.core.mvi.MviViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudyViewModel(
    deckId: String,
    studySessionRepository: StudySessionRepository,
    private val scheduleFlashcardReviewUseCase: ScheduleFlashcardReviewUseCase,
    private val flashcardReviewRepository: FlashcardReviewRepository,
) : MviViewModel<StudyUiState, StudyUiIntent, StudyUiEffect>(
    initialState = StudyUiState(),
) {

    private val studyItemsForToday: ArrayDeque<StudySessionItem> = ArrayDeque()
    private val pendingItemsByFlashcardId = mutableMapOf<FlashcardId, Int>()
    private val aggregatedGradesByFlashcardId = mutableMapOf<FlashcardId, ReviewGrade>()
    private val reviewsByFlashcardId = mutableMapOf<FlashcardId, FlashcardReview>()

    init {
        viewModelScope.launch {
            val studyFlashcards: List<StudyFlashcard> = studySessionRepository.sessionToday(deckId.toDeckId())
            val items = studyFlashcards.flatMap { sf ->
                reviewsByFlashcardId[sf.flashcardId] = sf.review
                pendingItemsByFlashcardId[sf.flashcardId] = sf.studyCards.count { it.isActive }
                sf.toStudySessionItems()
            }
            studyItemsForToday.addAll(items)
            mutableState.update { it.copy(totalCount = items.size) }
            showNextCard()
        }
    }

    private suspend fun showNextCard() {
        mutableState.update {
            if (studyItemsForToday.isNotEmpty()) {
                it.copy(currentItem = studyItemsForToday.removeFirstOrNull())
            } else {
                it.copy(currentItem = null)
            }
        }
        if (studyItemsForToday.isEmpty() && !mutableState.value.sessionFinished) {
            mutableState.update { it.copy(sessionFinished = true) }
            mutableEffect.send(StudyUiEffect.SessionFinished)
        }
    }

    private fun incrementReviewed() {
        mutableState.update { it.copy(reviewedCount = it.reviewedCount + 1) }
    }

    override fun onIntent(intent: StudyUiIntent) {
        when (intent) {
            StudyUiIntent.BackClicked,
            StudyUiIntent.FinishDialogDismissed -> {
                viewModelScope.launch { mutableEffect.send(StudyUiEffect.NavigateBack) }
            }
            is StudyUiIntent.ReviewAnswered -> processReviewAnswer(
                item = intent.item,
                reviewResult = intent.reviewGrade,
            )
        }
    }

    private fun processReviewAnswer(item: StudySessionItem?, reviewResult: ReviewGrade) = viewModelScope.launch {
        val flashcardId = item?.flashcardId ?: return@launch
        aggregatedGradesByFlashcardId[flashcardId] = moreConservativeGrade(
            current = aggregatedGradesByFlashcardId[flashcardId],
            incoming = reviewResult,
        )

        val remainingItems = pendingItemsByFlashcardId.getValue(flashcardId) - 1
        pendingItemsByFlashcardId[flashcardId] = remainingItems

        if (remainingItems == 0) {
            val finalGrade = aggregatedGradesByFlashcardId.remove(flashcardId) ?: reviewResult
            val persistedReview = reviewsByFlashcardId.getValue(flashcardId)
            val newReview: FlashcardReview = scheduleFlashcardReviewUseCase(
                review = persistedReview,
                grade = finalGrade,
                flashcardId = flashcardId,
            )
            flashcardReviewRepository.update(newReview)
            pendingItemsByFlashcardId.remove(flashcardId)
            reviewsByFlashcardId.remove(flashcardId)
        }

        incrementReviewed()
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

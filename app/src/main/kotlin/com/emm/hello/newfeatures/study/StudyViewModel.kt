package com.emm.hello.newfeatures.study

import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.GetStudySessionUseCase
import com.emm.domain.flashcard.UpdateFlashcardReviewUseCase
import com.emm.domain.study.ReviewGrade
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import com.emm.hello.core.mvi.MviViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudyViewModel(
    deckId: String,
    getStudySessionUseCase: GetStudySessionUseCase,
    private val scheduleFlashcardReviewUseCase: ScheduleFlashcardReviewUseCase,
    private val updateFlashcardReviewUseCase: UpdateFlashcardReviewUseCase,
) : MviViewModel<StudyUiState, StudyUiIntent, StudyUiEffect>(
    initialState = StudyUiState(),
) {

    private val studyItemsForToday: ArrayDeque<StudySessionItem> = ArrayDeque()
    private val pendingItemsByFlashcardId = mutableMapOf<String, Int>()
    private val aggregatedGradesByFlashcardId = mutableMapOf<String, ReviewGrade>()
    private val flashcardsById = mutableMapOf<String, Flashcard>()

    init {
        viewModelScope.launch {
            val flashcards: List<Flashcard> = getStudySessionUseCase(deckId)
            val items = flashcards.flatMap { flashcard ->
                flashcardsById[flashcard.id] = flashcard
                flashcard.toStudySessionItems().also { pendingItemsByFlashcardId[flashcard.id] = it.size }
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
        val flashcard = item?.flashcard ?: return@launch
        val flashcardId = flashcard.id
        aggregatedGradesByFlashcardId[flashcardId] = moreConservativeGrade(
            current = aggregatedGradesByFlashcardId[flashcardId],
            incoming = reviewResult,
        )

        val remainingItems = pendingItemsByFlashcardId.getValue(flashcardId) - 1
        pendingItemsByFlashcardId[flashcardId] = remainingItems

        if (remainingItems == 0) {
            val finalGrade = aggregatedGradesByFlashcardId.remove(flashcardId) ?: reviewResult
            val persistedFlashcard = flashcardsById.getValue(flashcardId)
            val newReview: FlashcardReview = scheduleFlashcardReviewUseCase(
                review = persistedFlashcard.review,
                grade = finalGrade,
                flashcardId = flashcardId,
            )
            updateFlashcardReviewUseCase(newReview)
            pendingItemsByFlashcardId.remove(flashcardId)
            flashcardsById.remove(flashcardId)
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

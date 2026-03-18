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

    private val flashcardsForToday: ArrayDeque<Flashcard> = ArrayDeque()

    init {
        viewModelScope.launch {
            val flashcards: List<Flashcard> = getStudySessionUseCase(deckId)
            flashcardsForToday.addAll(flashcards)
            mutableState.update { it.copy(totalCount = flashcards.size) }
            showNextCard()
        }
    }

    private suspend fun showNextCard() {
        mutableState.update {
            if (flashcardsForToday.isNotEmpty()) {
                it.copy(currentFlashcard = flashcardsForToday.removeFirstOrNull())
            } else {
                it.copy(currentFlashcard = null)
            }
        }
        if (flashcardsForToday.isEmpty() && !mutableState.value.sessionFinished) {
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
                flashcard = intent.flashcard,
                reviewResult = intent.reviewGrade,
            )
        }
    }

    private fun processReviewAnswer(flashcard: Flashcard?, reviewResult: ReviewGrade) = viewModelScope.launch {
        if (flashcard == null) return@launch

        val newReview: FlashcardReview = scheduleFlashcardReviewUseCase(
            review = flashcard.review,
            grade = reviewResult,
            flashcardId = flashcard.id,
        )
        updateFlashcardReviewUseCase(newReview)
        incrementReviewed()
        showNextCard()
    }
}

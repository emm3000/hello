package com.emm.hello.newfeatures.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.GetStudySessionUseCase
import com.emm.domain.flashcard.UpdateFlashcardReviewUseCase
import com.emm.domain.study.ReviewGrade
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudyViewModel(
    deckId: String,
    getStudySessionUseCase: GetStudySessionUseCase,
    private val scheduleFlashcardReviewUseCase: ScheduleFlashcardReviewUseCase,
    private val updateFlashcardReviewUseCase: UpdateFlashcardReviewUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyUiState())
    val uiState = _state.asStateFlow()

    private val _effect = Channel<StudyUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val flashcardsForToday: ArrayDeque<Flashcard> = ArrayDeque()
    private var hasEmittedSessionFinished = false

    init {
        viewModelScope.launch {
            val flashcards: List<Flashcard> = getStudySessionUseCase(deckId)
            flashcardsForToday.addAll(flashcards)
            _state.update { it.copy(totalCount = flashcards.size) }
            showNextCard()
        }
    }

    private suspend fun showNextCard() {
        _state.update {
            if (flashcardsForToday.isNotEmpty()) {
                it.copy(currentFlashcard = flashcardsForToday.removeFirstOrNull())
            } else {
                it.copy(currentFlashcard = null)
            }
        }
        if (flashcardsForToday.isEmpty() && !hasEmittedSessionFinished) {
            hasEmittedSessionFinished = true
            _effect.send(StudyUiEffect.SessionFinished)
        }
    }

    private fun incrementReviewed() {
        _state.update { it.copy(reviewedCount = it.reviewedCount + 1) }
    }

    fun onIntent(intent: StudyUiIntent) {
        when (intent) {
            StudyUiIntent.BackClicked,
            StudyUiIntent.FinishDialogDismissed -> {
                viewModelScope.launch { _effect.send(StudyUiEffect.NavigateBack) }
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

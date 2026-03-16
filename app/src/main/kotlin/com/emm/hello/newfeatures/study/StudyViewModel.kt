package com.emm.hello.newfeatures.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.GetStudySessionUseCase
import com.emm.domain.flashcard.UpdateFlashcardReviewUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudyViewModel(
    deckId: String,
    getStudySessionUseCase: GetStudySessionUseCase,
    private val updateFlashcardReviewUseCase: UpdateFlashcardReviewUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(StudyUiState())
    val state = _state.asStateFlow()

    private val flashcardsForToday: ArrayDeque<Flashcard> = ArrayDeque()

    init {
        viewModelScope.launch {
            val flashcards: List<Flashcard> = getStudySessionUseCase(deckId)
            flashcardsForToday.addAll(flashcards)
            _state.update { it.copy(totalCount = flashcards.size) }
            showNextCard()
        }
    }

    private fun showNextCard() {
        _state.update {
            if (flashcardsForToday.isNotEmpty()) {
                it.copy(currentFlashcard = flashcardsForToday.removeFirstOrNull())
            } else {
                it.copy(isFinished = true)
            }
        }
    }

    private fun incrementReviewed() {
        _state.update { it.copy(reviewedCount = it.reviewedCount + 1) }
    }

    fun onIntent(intent: StudyUiIntent) {
        when (intent) {
            is StudyUiIntent.ReviewAnswered -> processReviewAnswer(
                flashcard = intent.flashcard,
                reviewResult = intent.reviewGrade,
            )
        }
    }

    private fun processReviewAnswer(flashcard: Flashcard?, reviewResult: ReviewGrade) = viewModelScope.launch {
        if (flashcard == null) return@launch

        val newReview: FlashcardReview = SpacedRepetitionScheduler.schedule(
            review = flashcard.review,
            grade = reviewResult,
            flashcardId = flashcard.id,
        )
        updateFlashcardReviewUseCase(newReview)
        incrementReviewed()
        showNextCard()
    }
}

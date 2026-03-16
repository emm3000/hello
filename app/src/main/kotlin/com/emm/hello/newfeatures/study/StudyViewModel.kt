package com.emm.hello.newfeatures.study

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.GetStudySessionUseCase
import com.emm.domain.flashcard.UpdateFlashcardReviewUseCase
import kotlinx.coroutines.launch

class StudyViewModel(
    deckId: String,
    getStudySessionUseCase: GetStudySessionUseCase,
    private val updateFlashcardReviewUseCase: UpdateFlashcardReviewUseCase,
) : ViewModel() {

    var state by mutableStateOf(StudyUiState())
        private set

    private val flashcardsForToday: ArrayDeque<Flashcard> = ArrayDeque()

    init {
        viewModelScope.launch {
            val flashcards: List<Flashcard> = getStudySessionUseCase(deckId)
            flashcardsForToday.addAll(flashcards)
            state = state.copy(totalCount = flashcards.size)
            showNextCard()
        }
    }

    private fun showNextCard() {
        state = if (flashcardsForToday.isNotEmpty()) {
            state.copy(currentFlashcard = flashcardsForToday.removeFirstOrNull())
        } else {
            state.copy(isFinished = true)
        }
    }

    private fun incrementReviewed() {
        state = state.copy(reviewedCount = state.reviewedCount + 1)
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

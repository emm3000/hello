package com.emm.hello.newfeatures.study

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardFetcher
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.FlashcardReviewUpdater
import kotlinx.coroutines.launch

class StudyViewModel(
    deckId: String,
    flashcardFetcher: FlashcardFetcher,
    private val flashcardReviewUpdater: FlashcardReviewUpdater,
) : ViewModel() {

    var state by mutableStateOf(StudyUiState())
        private set

    private val flashcardsForToday: ArrayDeque<Flashcard> = ArrayDeque()

    init {
        viewModelScope.launch {
            val flashcards: List<Flashcard> = flashcardFetcher.fetchAll(deckId)
            flashcardsForToday.addAll(flashcards)
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

    fun onProcess(flashcard: Flashcard?, reviewResult: ReviewGrade) = viewModelScope.launch {
        if (flashcard == null) return@launch

        val newReview: FlashcardReview = SpacedRepetitionScheduler.schedule(
            review = flashcard.review,
            grade = reviewResult,
            flashcardId = flashcard.id,
        )
        flashcardReviewUpdater.update(newReview)
        showNextCard()
    }
}
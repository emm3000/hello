package com.emm.domain.study

import com.emm.domain.flashcard.FlashcardReview

class ScheduleFlashcardReviewUseCase {

    operator fun invoke(review: FlashcardReview, grade: ReviewGrade, flashcardId: String): FlashcardReview {
        return SpacedRepetitionScheduler.schedule(
            review = review,
            grade = grade,
            flashcardId = flashcardId,
        )
    }
}

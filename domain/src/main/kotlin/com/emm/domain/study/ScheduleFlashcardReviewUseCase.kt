package com.emm.domain.study

import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.ids.FlashcardId
import com.emm.domain.time.Clock

class ScheduleFlashcardReviewUseCase(private val clock: Clock) {

    operator fun invoke(review: FlashcardReview, grade: ReviewGrade, flashcardId: FlashcardId): FlashcardReview {
        return SpacedRepetitionScheduler.schedule(
            review = review,
            grade = grade,
            flashcardId = flashcardId,
            clock = clock,
        )
    }
}

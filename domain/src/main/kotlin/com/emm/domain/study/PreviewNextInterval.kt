package com.emm.domain.study

import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.time.Clock

object PreviewNextInterval {

    fun previewAll(review: FlashcardReview, clock: Clock): Map<ReviewGrade, Long> {
        return ReviewGrade.entries.associateWith { grade ->
            SpacedRepetitionScheduler.schedule(
                review = review,
                grade = grade,
                flashcardId = review.flashcardId,
                clock = clock,
            ).interval
        }
    }
}

package com.emm.domain.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsParameters
import com.emm.domain.ids.FlashcardId
import com.emm.domain.time.Clock

/**
 * FSRS-6 scheduling seam used by the app layer.
 *
 * params defaults to [FsrsParameters.DEFAULT] (retention 0.90); a future Settings screen
 * may inject per-user parameters here without changing this seam's shape.
 */
class ScheduleFlashcardReviewUseCase(
    private val clock: Clock,
    private val params: FsrsParameters = FsrsParameters.DEFAULT,
) {

    operator fun invoke(card: FsrsCard, grade: ReviewGrade, flashcardId: FlashcardId): FsrsCard =
        FsrsScheduler.schedule(
            card = card,
            grade = grade,
            flashcardId = flashcardId,
            clock = clock,
            params = params,
        )
}

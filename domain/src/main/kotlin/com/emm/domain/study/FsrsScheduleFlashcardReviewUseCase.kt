package com.emm.domain.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsParameters
import com.emm.domain.ids.FlashcardId
import com.emm.domain.time.Clock

/**
 * FSRS-6 replacement for ScheduleFlashcardReviewUseCase.
 *
 * PR1 introduces this use case alongside the old ScheduleFlashcardReviewUseCase.
 * The old use case remains until PR3 completes the type swap in :app.
 *
 * Phase 2 will inject FsrsParameters from Settings; for now DEFAULT is used.
 */
class FsrsScheduleFlashcardReviewUseCase(
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

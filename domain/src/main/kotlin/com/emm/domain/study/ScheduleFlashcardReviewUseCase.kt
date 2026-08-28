package com.emm.domain.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsParameters
import com.emm.domain.ids.FlashcardId
import com.emm.domain.time.Clock

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

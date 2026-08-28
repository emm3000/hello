package com.emm.domain.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsParameters
import com.emm.domain.flashcard.FsrsState
import com.emm.domain.ids.FlashcardId
import com.emm.domain.time.Clock

class ScheduleFlashcardReviewUseCase(
    private val clock: Clock,
    private val params: FsrsParameters = FsrsParameters.DEFAULT,
) {

    operator fun invoke(card: FsrsCard, grade: ReviewGrade, flashcardId: FlashcardId): FsrsCard {
        val scheduled: FsrsCard = FsrsScheduler.schedule(
            card = card,
            grade = grade,
            flashcardId = flashcardId,
            clock = clock,
            params = params,
        )
        val productionSince: Long? = card.productionSince ?: if (
            scheduled.state == FsrsState.REVIEW && scheduled.stability >= GRADUATION_STABILITY_DAYS
        ) {
            scheduled.lastReviewedAt
        } else {
            null
        }
        return scheduled.copy(productionSince = productionSince)
    }

    companion object {
        const val GRADUATION_STABILITY_DAYS: Double = 21.0
    }
}

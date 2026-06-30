package com.emm.domain.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsParameters
import com.emm.domain.time.Clock

/**
 * FSRS-6 backed interval preview.
 *
 * Returns the interval each grade would produce if applied to the given card right now.
 * Eliminates the SM-2 defect where early repetitions returned identical intervals for
 * all grades.
 */
object PreviewNextInterval {

    fun previewAll(
        card: FsrsCard,
        clock: Clock,
        params: FsrsParameters = FsrsParameters.DEFAULT,
    ): Map<ReviewGrade, Long> = ReviewGrade.entries.associateWith { grade ->
        FsrsScheduler.schedule(
            card = card,
            grade = grade,
            flashcardId = card.flashcardId,
            clock = clock,
            params = params,
        ).interval
    }
}

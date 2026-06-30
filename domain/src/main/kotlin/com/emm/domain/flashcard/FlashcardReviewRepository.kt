package com.emm.domain.flashcard

import com.emm.domain.study.ReviewGrade
import kotlinx.coroutines.flow.Flow

interface FlashcardReviewRepository {

    fun all(): Flow<List<FsrsCard>>

    /**
     * Persists the post-scheduling [card] state.
     *
     * [grade] is the real rating the user gave this review (AGAIN/HARD/GOOD/EASY); it is not
     * recoverable from [card] alone (FsrsCard is a state snapshot, not an event), so it must be
     * threaded explicitly to be written into the ReviewEvent's `rating` column.
     */
    suspend fun update(card: FsrsCard, grade: ReviewGrade)
}

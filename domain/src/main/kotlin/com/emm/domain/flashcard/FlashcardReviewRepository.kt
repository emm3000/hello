package com.emm.domain.flashcard

import com.emm.domain.study.ReviewGrade
import kotlinx.coroutines.flow.Flow

interface FlashcardReviewRepository {

    fun all(): Flow<List<FsrsCard>>

    // grade is not recoverable from card: FsrsCard is a state snapshot, not an event.
    suspend fun update(card: FsrsCard, grade: ReviewGrade)
}

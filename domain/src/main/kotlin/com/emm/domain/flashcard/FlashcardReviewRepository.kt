package com.emm.domain.flashcard

import kotlinx.coroutines.flow.Flow

interface FlashcardReviewRepository {

    fun all(): Flow<List<FlashcardReview>>

    suspend fun update(flashcardReview: FlashcardReview)
}

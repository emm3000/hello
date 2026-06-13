package com.emm.domain.flashcard

import kotlinx.coroutines.flow.Flow

/**
 * FSRS-6 replacement for FlashcardReviewRepository.
 *
 * PR1 introduces this interface in :domain alongside the old FlashcardReviewRepository.
 * The old interface remains until PR3 completes the compiler-driven type swap in :data/:app.
 */
interface FsrsFlashcardReviewRepository {

    fun all(): Flow<List<FsrsCard>>

    suspend fun update(card: FsrsCard)
}

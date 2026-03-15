package com.emm.data.flashcard

import android.content.Context
import com.emm.data.HelloDb
import com.emm.data.deck.RemoteDataSource

class FlashcardReviewSynchronizer(
    db: HelloDb,
    private val remote: RemoteDataSource,
    private val context: Context,
) {

    suspend fun execute() {
        // Legacy per-entity sync is disabled in local-first mode.
    }

    fun synchronize() {
        FlashcardReviewWorker.initialize(context)
    }
}

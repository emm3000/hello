package com.emm.data.quote

import android.content.Context
import com.emm.data.HelloDb
import com.emm.data.deck.RemoteDataSource

class QuoteSynchronizer(
    db: HelloDb,
    private val remote: RemoteDataSource,
    private val context: Context,
) {

    suspend fun execute() {
        // Legacy per-entity sync is disabled in local-first mode.
    }

    fun synchronize() {
        QuoteWorker.initialize(context)
    }
}

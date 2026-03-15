package com.emm.data.deck

import android.content.Context
import com.emm.data.HelloDb

class DeckSynchronizer(
    db: HelloDb,
    private val remote: RemoteDataSource,
    private val context: Context,
) {

    suspend fun execute() {
        // Legacy per-entity sync is disabled in local-first mode.
    }

    fun synchronize() {
        DeckWorker.initialize(context)
    }
}

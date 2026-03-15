package com.emm.data.flashcard

import com.emm.data.HelloDb
import com.emm.data.deck.RemoteDataSource

class ExampleSynchronizer(
    db: HelloDb,
    private val remote: RemoteDataSource,
) {

    suspend fun execute() {
        // Legacy per-entity sync is disabled in local-first mode.
    }
}

package com.emm.data.deck

import android.content.Context
import com.emm.data.DeckQueries
import com.emm.data.HelloDb

class DeckSynchronizer(
    db: HelloDb,
    private val remote: RemoteDataSource,
    private val context: Context,
) {

    private val dq: DeckQueries = db.deckQueries

    suspend fun execute() {
        val pendingDecks: List<DeckEntity> = dq.pending().executeAsList()

        if (pendingDecks.isEmpty()) return

        val deckRequests: List<CreateDeckRequest> = pendingDecks.map(::toRequest)
        remote.createDeck(deckRequests)
        val syncedDeckIds = pendingDecks.map(DeckEntity::id)
        dq.markAsSynced(syncedDeckIds)
    }

    fun synchronize() {
        DeckWorker.initialize(context)
    }
}

private fun toRequest(deck: DeckEntity): CreateDeckRequest = CreateDeckRequest(
    id = deck.id,
    name = deck.name,
    description = deck.description.orEmpty(),
    createdAt = deck.createdAt,
    updatedAt = deck.updatedAt
)
package com.emm.data.deck

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.DeckQueries
import com.emm.data.HelloDb
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

typealias DeckEntity = com.emm.data.Deck

class DefaultDeckRepository(db: HelloDb): DeckRepository {

    private val dq: DeckQueries = db.deckQueries

    override suspend fun addDeck(deck: CreateDeckInput) {
        dq.insert(
            id = UUID.randomUUID().toString(),
            name = deck.name,
            description = deck.description,
            createdAt = Instant.now().toEpochMilli(),
        )
    }

    override fun findById(deckId: String): Flow<Deck> {
        return dq
            .findById(deckId)
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map(DeckEntity::toDomain)
    }

    override fun fetchAll(): Flow<List<Deck>> = dq
        .all()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map(List<DeckEntity>::toDomain)
}
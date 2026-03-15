package com.emm.data.deck

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.DeckQueries
import com.emm.data.DeckWithFlashcardCount
import com.emm.data.HelloDb
import com.emm.data.localfirst.INITIAL_LAMPORT_VERSION
import com.emm.data.localfirst.LOCAL_DEVICE_ID
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

typealias DeckEntity = com.emm.data.Deck

class DefaultDeckRepository(
    db: HelloDb,
    private val synchronizer: DeckSynchronizer,
) : DeckRepository {

    private val dq: DeckQueries = db.deckQueries

    override suspend fun addDeck(deck: CreateDeckInput) = withContext(Dispatchers.IO) {
        val now: Long = Instant.now().toEpochMilli()
        val newId: String = UUID.randomUUID().toString()
        dq.insert(
            id = newId,
            name = deck.name,
            description = deck.description,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            originDeviceId = LOCAL_DEVICE_ID,
            lastModifiedByDeviceId = LOCAL_DEVICE_ID,
            versionLamport = INITIAL_LAMPORT_VERSION,
        )
        synchronizer.synchronize()
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

    override fun deckWithFlashcardCount(): Flow<List<Deck>> = dq
        .deckWithFlashcardCount()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map(::toDomain)
}

private fun toDomain(counts: List<DeckWithFlashcardCount>): List<Deck> = counts.map(::toDomain)

private fun toDomain(flashcardCount: DeckWithFlashcardCount): Deck = Deck(
    id = flashcardCount.id,
    name = flashcardCount.name,
    description = flashcardCount.description.orEmpty(),
    createdAt = flashcardCount.createdAt.toLocalDateTime(),
    cards = emptyList(),
    cardsCount = flashcardCount.flashcardCount,
)

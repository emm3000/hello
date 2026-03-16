package com.emm.data.deck

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.DeckQueries
import com.emm.data.DeckWithFlashcardCount
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.data.localfirst.OperationLogWriter
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.sync.OperationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

typealias DeckEntity = com.emm.data.Deck

@LocalFirstWrite
class DefaultDeckRepository(
    private val db: HelloDb,
    private val operationLogWriter: OperationLogWriter,
    private val localDeviceIdentityProvider: LocalDeviceIdentityProvider,
) : DeckRepository {

    private val dq: DeckQueries = db.deckQueries

    override suspend fun addDeck(deck: CreateDeckInput) = withContext(Dispatchers.IO) {
        val now: Long = Instant.now().toEpochMilli()
        val newId: String = UUID.randomUUID().toString()
        val deviceId = localDeviceIdentityProvider.getOrCreateDeviceId()

        db.transaction {
            val payloadJson = buildJsonObject {
                put("entityId", newId)
                put("operationType", OperationType.Create.name)
                put("name", deck.name)
                put("description", deck.description)
            }.toString()
            val lamport = operationLogWriter.appendOperation(
                entityType = "deck",
                entityId = newId,
                operationType = OperationType.Create,
                payloadJson = payloadJson,
                originDeviceId = deviceId,
                createdAt = now,
            )
            dq.insert(
                id = newId,
                name = deck.name,
                description = deck.description,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                originDeviceId = deviceId,
                lastModifiedByDeviceId = deviceId,
                versionLamport = lamport,
            )
        }
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

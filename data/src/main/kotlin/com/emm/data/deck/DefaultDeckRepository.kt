package com.emm.data.deck

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.DeckQueries
import com.emm.data.DeckWithFlashcardCount
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.data.logging.logInfo
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

@LocalFirstWrite
class DefaultDeckRepository(
    private val db: HelloDb,
) : DeckRepository {

    private val dq: DeckQueries = db.deckQueries

    override suspend fun addDeck(deck: CreateDeckInput) = withContext(Dispatchers.IO) {
        val now: Long = Instant.now().toEpochMilli()
        val newId: String = UUID.randomUUID().toString()
        logInfo(TAG, "addDeck:start deckId=$newId name=${deck.name}")

        db.transaction {
            dq.insert(
                id = newId,
                name = deck.name,
                description = deck.description,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            )
        }
        logInfo(TAG, "addDeck:success deckId=$newId")
        Unit
    }

    override fun findById(deckId: String): Flow<Deck> {
        return dq
            .findActiveById(deckId)
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map(DeckEntity::toDomain)
    }

    override fun fetchAll(): Flow<List<Deck>> {
        return dq
            .all()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(List<DeckEntity>::toDomain)
    }

    override fun deckWithFlashcardCount(): Flow<List<Deck>> {
        return dq
            .deckWithFlashcardCount()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(::toDomain)
    }
}

private const val TAG = "DeckRepository"

private fun toDomain(counts: List<DeckWithFlashcardCount>): List<Deck> = counts.map(::toDomain)

private fun toDomain(flashcardCount: DeckWithFlashcardCount): Deck = Deck(
    id = flashcardCount.id,
    name = flashcardCount.name,
    description = flashcardCount.description.orEmpty(),
    createdAt = flashcardCount.createdAt.toLocalDateTime(),
    cards = emptyList(),
    cardsCount = flashcardCount.flashcardCount,
)

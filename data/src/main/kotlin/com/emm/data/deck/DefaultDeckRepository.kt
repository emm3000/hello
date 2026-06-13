package com.emm.data.deck

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.DeckQueries
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckSearchCriteria
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.Tag
import com.emm.domain.deck.UpdateDeckInput
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toDeckId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

typealias DeckEntity = com.emm.data.Deck

@LocalFirstWrite
class DefaultDeckRepository(
    private val db: HelloDb,
    private val tagRepository: com.emm.domain.deck.TagRepository,
) : DeckRepository {

    private val dq: DeckQueries = db.deckQueries

    override suspend fun create(deck: CreateDeckInput) = withContext(Dispatchers.IO) {
        val now: Long = Instant.now().toEpochMilli()
        val newId: String = UUID.randomUUID().toString()

        db.transaction {
            dq.insert(
                id = newId,
                name = deck.name,
                description = deck.description,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            )

            if (deck.tags.isNotEmpty()) {
                val tq = db.tagQueries
                deck.tags.forEach { tagName ->
                    val normalized = tagName.lowercase().trim()
                    val existingTag = tq.findByName(normalized).executeAsOneOrNull()
                    val tagId = if (existingTag != null) {
                        existingTag.id
                    } else {
                        tq.getOrCreateTags(name = normalized, createdAt = now)
                        tq.findByName(normalized).executeAsOne().id
                    }
                    tq.insertDeckTag(
                        tagId = tagId,
                        deckId = newId,
                        createdAt = now,
                    )
                }
            }
        }
        Unit
    }

    override suspend fun update(input: UpdateDeckInput) = withContext(Dispatchers.IO) {
        require(input.name.isNotBlank()) { "Deck name must not be blank." }

        val now: Long = Instant.now().toEpochMilli()
        val deckId = input.deckId.value

        db.transactionWithResult {
            dq.findActiveById(deckId).executeAsOneOrNull()
                ?: throw NoSuchElementException("Deck not found or already deleted: $deckId")

            dq.update(
                name = input.name,
                description = input.description,
                updatedAt = now,
                id = deckId,
            )

            val tq = db.tagQueries
            val currentTags = tq.findByDeckId(deckId).executeAsList().map { it.id }

            val desiredTagIds = input.tags.map { tagName ->
                val normalized = tagName.lowercase().trim()
                val existingTag = tq.findByName(normalized).executeAsOneOrNull()
                if (existingTag != null) {
                    existingTag.id
                } else {
                    tq.getOrCreateTags(name = normalized, createdAt = now)
                    tq.findByName(normalized).executeAsOne().id
                }
            }

            if (currentTags.isNotEmpty() || desiredTagIds.isNotEmpty()) {
                if (desiredTagIds.isEmpty()) {
                    // SQLDelight binds an empty IN() as an error; "__none__" never exists → deletes all.
                    tq.deleteDeckTagsExcept(deckId = deckId, keepTagIds = listOf("__none__"))
                } else {
                    tq.deleteDeckTagsExcept(deckId = deckId, keepTagIds = desiredTagIds)
                }
                desiredTagIds.forEach { tagId ->
                    if (tagId !in currentTags) {
                        tq.insertDeckTag(tagId = tagId, deckId = deckId, createdAt = now)
                    }
                }
            }

            Unit
        }
    }

    override suspend fun softDeleteDeck(deckId: DeckId): Long = withContext(Dispatchers.IO) {
        val now: Long = Instant.now().toEpochMilli()

        db.transactionWithResult {
            dq.findActiveById(deckId.value).executeAsOneOrNull()
                ?: throw NoSuchElementException("Deck not found or already deleted: ${deckId.value}")

            // Cascade soft-delete (FK does not propagate deletedAt). Examples must go first:
            // softDeleteExamplesByDeck selects live flashcards, which stop matching once
            // softDeleteFlashcardsByDeck marks them deleted.
            dq.softDeleteExamplesByDeck(now = now, deckId = deckId.value)
            dq.softDeleteFlashcardsByDeck(now = now, deckId = deckId.value)
            dq.softDelete(now = now, id = deckId.value)

            now
        }
    }

    override suspend fun restoreDeck(deckId: DeckId, deletedAt: Long) = withContext(Dispatchers.IO) {
        db.transaction {
            // Restore only rows stamped with the exact cascade timestamp — rows deleted
            // earlier (different deletedAt) are intentionally left deleted.
            dq.restoreExamplesByDeck(deletedAt = deletedAt, deckId = deckId.value)
            dq.restoreFlashcardsByDeck(deckId = deckId.value, deletedAt = deletedAt)
            dq.restoreDeck(id = deckId.value, deletedAt = deletedAt)
        }
    }

    override fun fetchById(deckId: DeckId): Flow<Deck?> {
        val deckFlow = dq.findActiveById(deckId.value)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { entity -> entity?.toDomain() }
        val tagsFlow = tagRepository.fetchTagsForDeck(deckId)
        return combine(deckFlow, tagsFlow) { deck, tags ->
            deck?.copy(tags = tags)
        }
    }

    override fun fetchAll(): Flow<List<Deck>> {
        return dq
            .all()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map(DeckEntity::toDomain)
            }
    }

    override fun deckWithFlashcardCount(): Flow<List<Deck>> {
        return dq
            .deckWithFlashcardCount()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { counts ->
                counts.map { count ->
                    Deck(
                        id = count.id.toDeckId(),
                        name = count.name,
                        description = count.description.orEmpty(),
                        createdAt = count.createdAt.toLocalDateTime(),
                        cards = emptyList(),
                        cardsCount = count.flashcardCount,
                        tags = emptyList(), // Tags loaded per-deck via fetchTagsForDeck
                    )
                }
            }
    }

    override fun observeFiltered(criteria: DeckSearchCriteria): Flow<List<Deck>> {
        val normalizedTags = criteria.normalizedSelectedTags.toList()
        return dq.observeFiltered(
            query = criteria.normalizedQuery,
            selectedTagCount = normalizedTags.size.toLong(),
            normalizedTagNames = normalizedTags,
        )
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map { row ->
                    Deck(
                        id = row.id.toDeckId(),
                        name = row.name,
                        description = row.description.orEmpty(),
                        createdAt = row.createdAt.toLocalDateTime(),
                        cards = emptyList(),
                        cardsCount = row.flashcardCount,
                        tags = row.tagsCsv.toDomainTagsFromCsv(),
                    )
                }
            }
    }

    override fun fetchTagsForDeck(deckId: DeckId): Flow<List<Tag>> {
        return tagRepository.fetchTagsForDeck(deckId)
    }
}

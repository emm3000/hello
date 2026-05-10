package com.emm.data.deck

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.DeckQueries
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckSearchCriteria
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.Tag
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

    override suspend fun addDeck(deck: CreateDeckInput) = withContext(Dispatchers.IO) {
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

            // Insert tags and junction rows
            if (deck.tags.isNotEmpty()) {
                val tq = db.tagQueries
                deck.tags.forEach { tagName ->
                    val normalized = tagName.lowercase().trim()
                    // First check if tag already exists
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

    override fun findById(deckId: DeckId): Flow<Deck> {
        val deckFlow = dq.findActiveById(deckId.value)
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { entity -> entity.toDomain() }
        val tagsFlow = tagRepository.findTagsForDeck(deckId)
        return combine(deckFlow, tagsFlow) { deck, tags ->
            deck.copy(tags = tags)
        }
    }

    override fun fetchAll(): Flow<List<Deck>> {
        return dq
            .all()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toDomain() }
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
                        tags = emptyList(), // Tags loaded per-deck via findTagsForDeck
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

    override fun findTagsForDeck(deckId: DeckId): Flow<List<Tag>> {
        return tagRepository.findTagsForDeck(deckId)
    }
}

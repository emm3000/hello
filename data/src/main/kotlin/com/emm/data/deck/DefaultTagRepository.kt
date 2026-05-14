package com.emm.data.deck

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.HelloDb
import com.emm.data.TagQueries
import com.emm.domain.deck.Tag
import com.emm.domain.deck.TagRepository
import com.emm.domain.ids.DeckId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant

class DefaultTagRepository(
    private val db: HelloDb,
) : TagRepository {

    private val tq: TagQueries = db.tagQueries

    override suspend fun findOrCreate(tags: List<String>): List<Tag> = withContext(Dispatchers.IO) {
        val now = Instant.now().toEpochMilli()
        tags.map { tagName ->
            val normalized = tagName.lowercase().trim()
            tq.getOrCreateTags(
                name = normalized,
                createdAt = now,
            )
            Tag(value = normalized)
        }
    }

    override fun fetchTagsForDeck(deckId: DeckId): Flow<List<Tag>> {
        return tq
            .findByDeckId(deckId.value)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { tagEntities ->
                tagEntities.map { Tag(value = it.name) }
            }
    }
}

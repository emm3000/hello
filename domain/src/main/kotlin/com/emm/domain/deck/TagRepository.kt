package com.emm.domain.deck

import com.emm.domain.ids.DeckId
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    suspend fun findOrCreate(tags: List<String>): List<Tag>
    fun fetchTagsForDeck(deckId: DeckId): Flow<List<Tag>>
}

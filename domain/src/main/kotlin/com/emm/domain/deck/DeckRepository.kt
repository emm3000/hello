package com.emm.domain.deck

import com.emm.domain.ids.DeckId
import kotlinx.coroutines.flow.Flow

interface DeckRepository {

    suspend fun create(deck: CreateDeckInput)

    suspend fun update(input: UpdateDeckInput)

    suspend fun softDeleteDeck(deckId: DeckId)

    fun fetchById(deckId: DeckId): Flow<Deck>

    fun fetchAll(): Flow<List<Deck>>

    fun deckWithFlashcardCount(): Flow<List<Deck>>

    fun observeFiltered(criteria: DeckSearchCriteria): Flow<List<Deck>>

    fun fetchTagsForDeck(deckId: DeckId): Flow<List<Tag>>
}

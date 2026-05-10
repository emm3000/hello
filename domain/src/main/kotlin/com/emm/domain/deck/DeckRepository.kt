package com.emm.domain.deck

import com.emm.domain.ids.DeckId
import kotlinx.coroutines.flow.Flow

interface DeckRepository {

    suspend fun addDeck(deck: CreateDeckInput)

    fun findById(deckId: DeckId): Flow<Deck>

    fun fetchAll(): Flow<List<Deck>>

    fun deckWithFlashcardCount(): Flow<List<Deck>>

    fun observeFiltered(criteria: DeckSearchCriteria): Flow<List<Deck>>

    fun findTagsForDeck(deckId: DeckId): Flow<List<Tag>>
}

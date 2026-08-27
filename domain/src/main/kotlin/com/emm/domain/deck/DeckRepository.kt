package com.emm.domain.deck

import com.emm.domain.ids.DeckId
import kotlinx.coroutines.flow.Flow

interface DeckRepository {

    suspend fun create(deck: CreateDeckInput)

    suspend fun update(input: UpdateDeckInput)

    suspend fun softDeleteDeck(deckId: DeckId): Long

    suspend fun restoreDeck(deckId: DeckId, deletedAt: Long)

    fun fetchById(deckId: DeckId): Flow<Deck?>

    fun fetchAll(): Flow<List<Deck>>

    fun deckWithFlashcardCount(): Flow<List<Deck>>
}

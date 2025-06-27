package com.emm.domain.deck

import kotlinx.coroutines.flow.Flow

interface DeckRepository {

    suspend fun addDeck(deck: CreateDeckInput)

    fun findById(deckId: String): Flow<Deck>

    fun fetchAll(): Flow<List<Deck>>
}
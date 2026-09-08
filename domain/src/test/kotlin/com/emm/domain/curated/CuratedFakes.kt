package com.emm.domain.curated

import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.UpdateDeckInput
import com.emm.domain.ids.DeckId
import com.emm.domain.time.SystemClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeCuratedDeckCatalog(
    private val catalogDecks: List<CuratedDeck>,
) : CuratedDeckCatalog {

    override fun decks(): List<CuratedDeck> = catalogDecks
}

class FakeDeckRepository(
    private val liveDecks: List<Deck> = emptyList(),
) : DeckRepository {

    val created: MutableMap<DeckId, CreateDeckInput> = mutableMapOf()

    var createCalls: Int = 0
        private set

    override suspend fun create(deck: CreateDeckInput) {
        createCalls += 1
        created[requireNotNull(deck.id)] = deck
    }

    override fun fetchById(deckId: DeckId): Flow<Deck?> {
        val input: CreateDeckInput? = created[deckId]
        return flowOf(input?.let { Deck.empty(SystemClock).copy(id = deckId, name = it.name) })
    }

    override fun deckWithFlashcardCount(): Flow<List<Deck>> = flowOf(liveDecks)

    override suspend fun update(input: UpdateDeckInput) = throw UnsupportedOperationException()

    override suspend fun softDeleteDeck(deckId: DeckId): Long = throw UnsupportedOperationException()

    override suspend fun restoreDeck(deckId: DeckId, deletedAt: Long) = throw UnsupportedOperationException()

    override fun fetchAll(): Flow<List<Deck>> = throw UnsupportedOperationException()
}

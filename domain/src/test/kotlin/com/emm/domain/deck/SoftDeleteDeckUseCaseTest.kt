package com.emm.domain.deck

import com.emm.domain.ids.DeckId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SoftDeleteDeckUseCaseTest {

    @Test
    fun `deleting the last live deck is refused`() = runTest {
        val onlyDeck: Deck = deck("starter")
        val repository = FakeDeckRepository(listOf(onlyDeck))

        assertFailsWith<LastDeckDeletionException> { SoftDeleteDeckUseCase(repository)(onlyDeck.id) }

        assertNull(repository.softDeletedDeckId)
    }

    @Test
    fun `deleting one of two live decks soft deletes it`() = runTest {
        val starter: Deck = deck("starter")
        val travel: Deck = deck("travel")
        val repository = FakeDeckRepository(listOf(starter, travel))

        val deletedAt: Long = SoftDeleteDeckUseCase(repository)(travel.id)

        assertEquals(DELETED_AT, deletedAt)
        assertEquals(travel.id, repository.softDeletedDeckId)
    }

    private fun deck(id: String): Deck = Deck(
        id = DeckId.from(id),
        name = id,
        description = "",
        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        cards = emptyList(),
        cardsCount = 0L,
    )

    private class FakeDeckRepository(
        private val liveDecks: List<Deck>,
    ) : DeckRepository {

        var softDeletedDeckId: DeckId? = null
            private set

        override suspend fun softDeleteDeck(deckId: DeckId): Long {
            softDeletedDeckId = deckId
            return DELETED_AT
        }

        override fun fetchAll(): Flow<List<Deck>> = flowOf(liveDecks)

        override suspend fun create(deck: CreateDeckInput) = throw UnsupportedOperationException()

        override suspend fun update(input: UpdateDeckInput) = throw UnsupportedOperationException()

        override suspend fun restoreDeck(deckId: DeckId, deletedAt: Long) = throw UnsupportedOperationException()

        override fun fetchById(deckId: DeckId): Flow<Deck?> = throw UnsupportedOperationException()

        override fun deckWithFlashcardCount(): Flow<List<Deck>> = throw UnsupportedOperationException()
    }
}

private const val DELETED_AT: Long = 1_700_000_000_000L

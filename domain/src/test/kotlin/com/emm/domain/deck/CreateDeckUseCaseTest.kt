package com.emm.domain.deck

import com.emm.domain.time.Clock
import com.emm.domain.ids.DeckId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CreateDeckUseCaseTest {

    @Test
    fun `invoke delegates input to repository`() = runBlocking {
        val repository = FakeDeckRepository()
        val useCase = CreateDeckUseCase(repository)
        val input = CreateDeckInput(name = "Basics", description = "Starter deck")

        useCase(input)

        assertNotNull(repository.lastAddedDeck)
        assertEquals(input, repository.lastAddedDeck)
    }

    @Test
    fun `invoke can be called multiple times`() = runBlocking {
        val repository = FakeDeckRepository()
        val useCase = CreateDeckUseCase(repository)

        useCase(CreateDeckInput(name = "A1", description = ""))
        useCase(CreateDeckInput(name = "A2", description = ""))

        assertEquals(2, repository.addDeckCalls)
        assertEquals("A2", repository.lastAddedDeck?.name)
    }

    private class FakeDeckRepository : DeckRepository {
        var addDeckCalls: Int = 0
        var lastAddedDeck: CreateDeckInput? = null

        override suspend fun addDeck(deck: CreateDeckInput) {
            addDeckCalls += 1
            lastAddedDeck = deck
        }

        override fun findById(deckId: DeckId): Flow<Deck> = flowOf(Deck.empty(Clock { java.time.Instant.EPOCH }))

        override fun fetchAll(): Flow<List<Deck>> = flowOf(emptyList())

        override fun deckWithFlashcardCount(): Flow<List<Deck>> = flowOf(emptyList())
    }
}

package com.emm.domain.deck

import com.emm.domain.ids.DeckId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class GetFilteredDecksUseCaseTest {

    @Test
    fun `invoke delegates normalized criteria to repository`() = runTest {
        val fakeRepository = FakeDeckRepository()
        val useCase = GetFilteredDecksUseCase(fakeRepository)
        val criteria = DeckSearchCriteria(
            query = "  Bio  ",
            selectedTags = setOf(" Exam ", "biology", "BIOLOGY"),
        )

        val result = useCase(criteria).first()

        assertTrue(result.isEmpty())
        val captured = fakeRepository.lastObservedCriteria
        assertEquals("bio", captured?.normalizedQuery)
        assertEquals(setOf("exam", "biology"), captured?.normalizedSelectedTags)
    }

    private class FakeDeckRepository : DeckRepository {
        var lastObservedCriteria: DeckSearchCriteria? = null

        override suspend fun addDeck(deck: CreateDeckInput) = Unit

        override fun findById(deckId: DeckId): Flow<Deck> = flowOf(
            Deck(
                id = DeckId.from("deck-1"),
                name = "Deck",
                description = "",
                createdAt = LocalDateTime.now(),
                cards = emptyList(),
                cardsCount = 0,
                tags = emptyList(),
            ),
        )

        override fun fetchAll(): Flow<List<Deck>> = flowOf(emptyList())

        override fun deckWithFlashcardCount(): Flow<List<Deck>> = flowOf(emptyList())

        override fun observeFiltered(criteria: DeckSearchCriteria): Flow<List<Deck>> {
            lastObservedCriteria = criteria
            return flowOf(emptyList())
        }

        override fun findTagsForDeck(deckId: DeckId): Flow<List<Tag>> = flowOf(emptyList())

        override suspend fun updateDeck(input: UpdateDeckInput) = Unit
        override suspend fun softDeleteDeck(deckId: DeckId) = Unit
    }
}

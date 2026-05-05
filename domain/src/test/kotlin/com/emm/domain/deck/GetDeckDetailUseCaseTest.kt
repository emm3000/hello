package com.emm.domain.deck

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReadRepository
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDeckDetailUseCaseTest {

    @Test
    fun `invoke combines deck and cards without re-fetching each card`() = runTest {
        val deckRepository = FakeDeckRepository()
        val flashcardRepository = FakeFlashcardReadRepository()
        val useCase = GetDeckDetailUseCase(deckRepository, flashcardRepository)

        val result = useCase("deck-1").first()

        assertEquals(2, result.cards.size)
        assertEquals(0, flashcardRepository.fetchByIdCalls)
    }
}

private class FakeDeckRepository : DeckRepository {
    override suspend fun addDeck(deck: CreateDeckInput) = Unit

    override fun findById(deckId: String): Flow<Deck> {
        return flowOf(
            Deck(
                id = deckId,
                name = "Main",
                description = "",
                createdAt = Deck.empty(Clock { Instant.EPOCH }).createdAt,
                cards = emptyList(),
                cardsCount = 0L,
            )
        )
    }

    override fun fetchAll(): Flow<List<Deck>> = flowOf(emptyList())

    override fun deckWithFlashcardCount(): Flow<List<Deck>> = flowOf(emptyList())
}

private class FakeFlashcardReadRepository : FlashcardReadRepository {
    var fetchByIdCalls: Int = 0

    override fun fetchAll(): Flow<List<Flashcard>> = flowOf(emptyList())

    override fun fetchByDeckId(deckId: String): Flow<List<Flashcard>> {
        return flowOf(
            listOf(
                sampleCard(id = "card-1"),
                sampleCard(id = "card-2"),
            )
        )
    }

    override suspend fun fetchById(id: String): Flashcard {
        fetchByIdCalls += 1
        return sampleCard(id = id)
    }

    private fun sampleCard(id: String): Flashcard {
        return Flashcard(
            id = id,
            word = "borrow",
            meaning = "to take and return",
            translation = "pedir prestado",
            examples = emptyList(),
            phonetic = "",
            review = FlashcardReview.empty(Clock { Instant.EPOCH }),
        )
    }
}

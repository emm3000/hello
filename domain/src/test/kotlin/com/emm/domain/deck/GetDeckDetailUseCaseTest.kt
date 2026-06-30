package com.emm.domain.deck

import app.cash.turbine.test
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDeckDetailUseCaseTest {

    @Test
    fun `invoke combines deck and cards without re-fetching each card`() = runTest {
        val deckRepository = FakeDeckRepository(
            deckFlow = MutableStateFlow(
                Deck(
                    id = "deck-1".toDeckId(),
                    name = "Main",
                    description = "",
                    createdAt = Deck.empty(Clock { Instant.EPOCH }).createdAt,
                    cards = emptyList(),
                    cardsCount = 0L,
                )
            )
        )
        val flashcardRepository = FakeFlashcardRepository(
            cardsFlow = MutableStateFlow(
                listOf(
                    sampleCard(id = "card-1"),
                    sampleCard(id = "card-2"),
                )
            )
        )
        val useCase = GetDeckDetailUseCase(deckRepository, flashcardRepository)

        useCase("deck-1").test {
            val result = awaitItem()!!
            assertEquals(2, result.cards.size)
            assertEquals("deck-1", deckRepository.lastFindByIdArg?.value)
            assertEquals("deck-1", flashcardRepository.lastFetchByDeckIdArg?.value)
            assertEquals(0, flashcardRepository.fetchByIdCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invoke emits updated deck detail when cards flow changes`() = runTest {
        val deckFlow = MutableStateFlow(
            Deck(
                id = "deck-1".toDeckId(),
                name = "Main",
                description = "Core deck",
                createdAt = Deck.empty(Clock { Instant.EPOCH }).createdAt,
                cards = emptyList(),
                cardsCount = 0L,
            )
        )
        val cardsFlow = MutableStateFlow(listOf(sampleCard(id = "card-1")))

        val deckRepository = FakeDeckRepository(deckFlow = deckFlow)
        val flashcardRepository = FakeFlashcardRepository(cardsFlow = cardsFlow)
        val useCase = GetDeckDetailUseCase(deckRepository, flashcardRepository)

        useCase("deck-1").test {
            val first = awaitItem()!!
            assertEquals("deck-1", first.id.value)
            assertEquals("Main", first.name)
            assertEquals(1, first.cards.size)

            cardsFlow.value = listOf(
                sampleCard(id = "card-1"),
                sampleCard(id = "card-2"),
            )

            val second = awaitItem()!!
            assertEquals("deck-1", second.id.value)
            assertEquals("Main", second.name)
            assertEquals(2, second.cards.size)
            assertEquals("deck-1", deckRepository.lastFindByIdArg?.value)
            assertEquals("deck-1", flashcardRepository.lastFetchByDeckIdArg?.value)
            assertEquals(0, flashcardRepository.fetchByIdCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke rejects blank deck id`() = runTest {
        val deckRepository = FakeDeckRepository(
            deckFlow = MutableStateFlow(
                Deck(
                    id = "deck-1".toDeckId(),
                    name = "Main",
                    description = "",
                    createdAt = Deck.empty(Clock { Instant.EPOCH }).createdAt,
                    cards = emptyList(),
                    cardsCount = 0L,
                )
            )
        )
        val flashcardRepository = FakeFlashcardRepository(
            cardsFlow = MutableStateFlow(listOf(sampleCard(id = "card-1")))
        )
        val useCase = GetDeckDetailUseCase(deckRepository, flashcardRepository)

        useCase("   ")
    }
}

private class FakeDeckRepository : DeckRepository {
    constructor(deckFlow: MutableStateFlow<Deck>) {
        this.deckFlow = deckFlow
    }

    private var deckFlow: MutableStateFlow<Deck> = MutableStateFlow(
        Deck(
            id = "deck-1".toDeckId(),
            name = "Main",
            description = "",
            createdAt = Deck.empty(Clock { Instant.EPOCH }).createdAt,
            cards = emptyList(),
            cardsCount = 0L,
        )
    )

    var lastFindByIdArg: com.emm.domain.ids.DeckId? = null

    override suspend fun create(deck: CreateDeckInput) = Unit

    override fun fetchById(deckId: DeckId): Flow<Deck> {
        lastFindByIdArg = deckId
        return deckFlow
    }

    override fun fetchAll(): Flow<List<Deck>> = flowOf(emptyList())

    override fun deckWithFlashcardCount(): Flow<List<Deck>> = flowOf(emptyList())

    override fun observeFiltered(criteria: DeckSearchCriteria): Flow<List<Deck>> = flowOf(emptyList())

    override fun fetchTagsForDeck(deckId: DeckId): Flow<List<Tag>> = emptyFlow()

    override suspend fun update(input: UpdateDeckInput) = Unit
    override suspend fun softDeleteDeck(deckId: DeckId): Long = 0L
    override suspend fun restoreDeck(deckId: DeckId, deletedAt: Long) = Unit
}

private class FakeFlashcardRepository : FlashcardRepository {
    constructor(cardsFlow: MutableStateFlow<List<Flashcard>>) {
        this.cardsFlow = cardsFlow
    }

    private var cardsFlow: MutableStateFlow<List<Flashcard>> = MutableStateFlow(
        listOf(
            sampleCard(id = "card-1"),
            sampleCard(id = "card-2"),
        )
    )

    var lastFetchByDeckIdArg: com.emm.domain.ids.DeckId? = null

    var fetchByIdCalls: Int = 0

    override fun fetchAll(): Flow<List<Flashcard>> = flowOf(emptyList())

    override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> {
        lastFetchByDeckIdArg = deckId
        return cardsFlow
    }

    override suspend fun fetchById(id: FlashcardId): FlashcardDetail {
        fetchByIdCalls += 1
        return FlashcardDetail(flashcard = sampleCard(id = id.value))
    }

    override suspend fun create(input: CreateFlashcardInput): FlashcardId = throw UnsupportedOperationException()
    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) = Unit

    override suspend fun update(input: UpdateFlashcardInput) = Unit
    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = 0L
    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = Unit
    override suspend fun countDueFlashcards(nowMillis: Long): Long = 0L
}

private fun sampleCard(id: String): Flashcard {
    val flashcardId = id.toFlashcardId()
    return Flashcard(
        id = flashcardId,
        word = "borrow",
        meaning = "to take and return",
        translation = "pedir prestado",
        examples = emptyList(),
        phonetic = "",
        review = FsrsCard.new(flashcardId, Clock { Instant.EPOCH }),
    )
}

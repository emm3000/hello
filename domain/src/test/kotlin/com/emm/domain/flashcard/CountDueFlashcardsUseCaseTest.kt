package com.emm.domain.flashcard

import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CountDueFlashcardsUseCaseTest {

    private val now = Instant.parse("2026-05-16T19:00:00Z")
    private val clock = Clock { now }

    @Test
    fun `returns count from repository at clock now`() = runTest {
        val repo = StubFlashcardRepository(dueCount = 7L)
        val useCase = CountDueFlashcardsUseCase(repo, clock)

        val result = useCase()

        assertEquals(7L, result)
        assertEquals(now.toEpochMilli(), repo.lastQueriedNow)
    }

    @Test
    fun `zero when nothing is due`() = runTest {
        val repo = StubFlashcardRepository(dueCount = 0L)
        val useCase = CountDueFlashcardsUseCase(repo, clock)

        assertEquals(0L, useCase())
    }
}

private class StubFlashcardRepository(private val dueCount: Long) : FlashcardRepository {
    var lastQueriedNow: Long = -1L
        private set

    override fun fetchAll(): Flow<List<Flashcard>> = flowOf(emptyList())
    override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> = flowOf(emptyList())
    override suspend fun fetchById(id: FlashcardId): FlashcardDetail = error("not used")
    override suspend fun create(input: CreateFlashcardInput): FlashcardId = error("not used")
    override suspend fun update(input: UpdateFlashcardInput) = error("not used")
    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId) = error("not used")
    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) = error("not used")
    override suspend fun countDueFlashcards(nowMillis: Long): Long {
        lastQueriedNow = nowMillis
        return dueCount
    }
}

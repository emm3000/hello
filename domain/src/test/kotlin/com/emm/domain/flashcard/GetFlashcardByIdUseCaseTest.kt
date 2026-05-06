package com.emm.domain.flashcard

import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.time.SystemClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFlashcardByIdUseCaseTest {

    @Test
    fun `invoke normalizes id before querying repository`() = runTest {
        val repository = FakeFlashcardReadRepository()
        val useCase = GetFlashcardByIdUseCase(repository)

        useCase("  card-1  ")

        assertEquals("card-1", repository.lastFetchByIdArg)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke rejects blank id`() = runTest {
        val useCase = GetFlashcardByIdUseCase(FakeFlashcardReadRepository())

        useCase("   ")
    }
}

private class FakeFlashcardReadRepository : FlashcardReadRepository {
    var lastFetchByIdArg: String? = null

    override fun fetchAll(): Flow<List<Flashcard>> = emptyFlow()

    override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> = emptyFlow()

    override suspend fun fetchById(id: FlashcardId): FlashcardDetail {
        lastFetchByIdArg = id.value
        return FlashcardDetail(flashcard = Flashcard.empty(SystemClock).copy(id = id))
    }
}

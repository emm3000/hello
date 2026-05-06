package com.emm.domain.study

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.ids.DeckId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetStudySessionUseCaseTest {

    @Test
    fun `invoke normalizes deck id before querying repository`() = runTest {
        val repository = FakeStudySessionForGetRepository()
        val useCase = GetStudySessionUseCase(repository)

        useCase("  deck-1  ")

        assertEquals("deck-1", repository.lastSessionTodayDeckId?.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke rejects blank deck id`() = runTest {
        val useCase = GetStudySessionUseCase(FakeStudySessionForGetRepository())

        useCase("   ")
    }
}

private class FakeStudySessionForGetRepository : StudySessionRepository {
    var lastSessionTodayDeckId: DeckId? = null

    override suspend fun sessionToday(deckId: DeckId): List<Flashcard> {
        lastSessionTodayDeckId = deckId
        return emptyList()
    }

    override fun flashcardWithReview(deckId: DeckId): Flow<List<Flashcard>> = emptyFlow()
}

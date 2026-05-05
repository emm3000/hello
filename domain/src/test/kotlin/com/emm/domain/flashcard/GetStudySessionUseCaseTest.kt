package com.emm.domain.flashcard

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

        assertEquals("deck-1", repository.lastSessionTodayDeckId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke rejects blank deck id`() = runTest {
        val useCase = GetStudySessionUseCase(FakeStudySessionForGetRepository())

        useCase("   ")
    }
}

private class FakeStudySessionForGetRepository : StudySessionRepository {
    var lastSessionTodayDeckId: String? = null

    override suspend fun sessionToday(deckId: String): List<Flashcard> {
        lastSessionTodayDeckId = deckId
        return emptyList()
    }

    override fun flashcardWithReview(deckId: String): Flow<List<Flashcard>> = emptyFlow()
}

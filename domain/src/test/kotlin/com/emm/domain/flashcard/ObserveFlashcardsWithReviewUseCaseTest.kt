package com.emm.domain.flashcard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveFlashcardsWithReviewUseCaseTest {

    @Test
    fun `invoke normalizes deck id before observing repository flow`() {
        val repository = FakeStudySessionForObserveRepository()
        val useCase = ObserveFlashcardsWithReviewUseCase(repository)

        useCase("  deck-1  ")

        assertEquals("deck-1", repository.lastFlashcardWithReviewDeckId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke rejects blank deck id`() {
        val useCase = ObserveFlashcardsWithReviewUseCase(FakeStudySessionForObserveRepository())

        useCase("   ")
    }
}

private class FakeStudySessionForObserveRepository : StudySessionRepository {
    var lastFlashcardWithReviewDeckId: String? = null

    override suspend fun sessionToday(deckId: String): List<Flashcard> = emptyList()

    override fun flashcardWithReview(deckId: String): Flow<List<Flashcard>> {
        lastFlashcardWithReviewDeckId = deckId
        return emptyFlow()
    }
}

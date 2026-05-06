package com.emm.domain.study

import com.emm.domain.ids.DeckId
import com.emm.domain.study.StudyFlashcard
import com.emm.domain.study.StudySessionRepository
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

        assertEquals("deck-1", repository.lastFlashcardWithReviewDeckId?.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke rejects blank deck id`() {
        val useCase = ObserveFlashcardsWithReviewUseCase(FakeStudySessionForObserveRepository())

        useCase("   ")
    }
}

private class FakeStudySessionForObserveRepository : StudySessionRepository {
    var lastFlashcardWithReviewDeckId: DeckId? = null

    override suspend fun sessionToday(deckId: DeckId): List<StudyFlashcard> = emptyList()

    override fun flashcardWithReview(deckId: DeckId): Flow<List<StudyFlashcard>> {
        lastFlashcardWithReviewDeckId = deckId
        return emptyFlow()
    }
}

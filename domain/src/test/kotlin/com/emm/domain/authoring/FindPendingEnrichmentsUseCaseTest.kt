package com.emm.domain.authoring

import com.emm.domain.flashcard.EnrichmentBacklog
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.FlashcardEnrichmentRepository
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FindPendingEnrichmentsUseCaseTest {

    @Test
    fun `invoke returns the pending flashcards`() = runTest {
        val repository = RecordingPendingEnrichmentRepository(pending = listOf("card-1", "card-2"))
        val useCase = FindPendingEnrichmentsUseCase(repository)

        val pending: List<FlashcardId> = useCase()

        assertEquals(listOf("card-1".toFlashcardId(), "card-2".toFlashcardId()), pending)
    }

    @Test
    fun `invoke only asks for the pending ones`() = runTest {
        val repository = RecordingPendingEnrichmentRepository(pending = listOf("card-1"))
        val useCase = FindPendingEnrichmentsUseCase(repository)

        useCase()

        assertEquals(listOf(EnrichmentStatus.PENDING), repository.requestedStatuses)
    }

    @Test
    fun `invoke returns nothing when nothing is pending`() = runTest {
        val repository = RecordingPendingEnrichmentRepository(pending = emptyList())
        val useCase = FindPendingEnrichmentsUseCase(repository)

        val pending: List<FlashcardId> = useCase()

        assertEquals(emptyList(), pending)
    }
}

private class RecordingPendingEnrichmentRepository(
    private val pending: List<String>,
) : FlashcardEnrichmentRepository {

    val requestedStatuses: MutableList<EnrichmentStatus> = mutableListOf()

    override fun observeBacklog(): Flow<EnrichmentBacklog> = flowOf(EnrichmentBacklog())

    override suspend fun findIdsByStatus(status: EnrichmentStatus): List<FlashcardId> {
        requestedStatuses += status
        return pending.map(String::toFlashcardId)
    }

    override suspend fun markPending(ids: List<FlashcardId>) = Unit
}

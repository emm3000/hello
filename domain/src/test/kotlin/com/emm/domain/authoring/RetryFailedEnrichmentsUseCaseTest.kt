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

class RetryFailedEnrichmentsUseCaseTest {

    @Test
    fun `invoke returns the failed flashcards`() = runTest {
        val repository = RecordingEnrichmentRepository(failed = listOf("card-1", "card-2"))
        val useCase = RetryFailedEnrichmentsUseCase(repository)

        val retried: List<FlashcardId> = useCase()

        assertEquals(listOf("card-1".toFlashcardId(), "card-2".toFlashcardId()), retried)
    }

    @Test
    fun `invoke moves the failed flashcards back to pending`() = runTest {
        val repository = RecordingEnrichmentRepository(failed = listOf("card-1", "card-2"))
        val useCase = RetryFailedEnrichmentsUseCase(repository)

        useCase()

        assertEquals(listOf("card-1".toFlashcardId(), "card-2".toFlashcardId()), repository.markedPending)
    }

    @Test
    fun `invoke only asks for the failed ones`() = runTest {
        val repository = RecordingEnrichmentRepository(failed = listOf("card-1"))
        val useCase = RetryFailedEnrichmentsUseCase(repository)

        useCase()

        assertEquals(listOf(EnrichmentStatus.FAILED), repository.requestedStatuses)
    }

    @Test
    fun `invoke touches nothing when there is nothing to retry`() = runTest {
        val repository = RecordingEnrichmentRepository(failed = emptyList())
        val useCase = RetryFailedEnrichmentsUseCase(repository)

        val retried: List<FlashcardId> = useCase()

        assertEquals(emptyList(), retried)
        assertEquals(0, repository.markPendingCalls)
    }
}

private class RecordingEnrichmentRepository(
    private val failed: List<String>,
) : FlashcardEnrichmentRepository {

    val requestedStatuses: MutableList<EnrichmentStatus> = mutableListOf()
    val markedPending: MutableList<FlashcardId> = mutableListOf()
    var markPendingCalls: Int = 0

    override fun observeBacklog(): Flow<EnrichmentBacklog> = flowOf(EnrichmentBacklog())

    override suspend fun findIdsByStatus(status: EnrichmentStatus): List<FlashcardId> {
        requestedStatuses += status
        return failed.map(String::toFlashcardId)
    }

    override suspend fun markPending(ids: List<FlashcardId>) {
        markPendingCalls += 1
        markedPending += ids
    }
}

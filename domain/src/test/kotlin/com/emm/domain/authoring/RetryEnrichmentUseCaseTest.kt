package com.emm.domain.authoring

import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardEnrichmentRepository
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.generation.EnrichmentFailureCause
import com.emm.domain.generation.GenerationCredits
import com.emm.domain.generation.GenerationCreditsRepository
import com.emm.domain.generation.InputProblem
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.Clock
import com.emm.domain.time.SystemClock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RetryEnrichmentUseCaseTest {

    @Test
    fun `a technical failure moves only that card back to pending`() = runTest {
        val enrichment = RecordingEnrichmentRepository()
        val useCase: RetryEnrichmentUseCase = useCaseFor(EnrichmentFailureCause.Technical, enrichment)

        val retried: Boolean = useCase(FLASHCARD_ID)

        assertTrue(retried)
        assertEquals(listOf(listOf(FLASHCARD_ID)), enrichment.markedPending)
    }

    @Test
    fun `an app check rejection is not retried`() = runTest {
        val enrichment = RecordingEnrichmentRepository()
        val useCase: RetryEnrichmentUseCase = useCaseFor(EnrichmentFailureCause.AppCheckRejected, enrichment)

        assertFalse(useCase(FLASHCARD_ID))
        assertEquals(emptyList(), enrichment.markedPending)
    }

    @Test
    fun `a word problem is not retried`() = runTest {
        val enrichment = RecordingEnrichmentRepository()
        val useCase: RetryEnrichmentUseCase = useCaseFor(
            EnrichmentFailureCause.WordProblem(InputProblem.Unintelligible),
            enrichment,
        )

        assertFalse(useCase(FLASHCARD_ID))
        assertEquals(emptyList(), enrichment.markedPending)
    }

    @Test
    fun `exhausted credits are not retried before the reset`() = runTest {
        val enrichment = RecordingEnrichmentRepository()
        val useCase: RetryEnrichmentUseCase = useCaseFor(
            EnrichmentFailureCause.CreditsExhausted,
            enrichment,
            credits = GenerationCredits(remaining = 0, resetAt = NOW.plusSeconds(60)),
        )

        assertFalse(useCase(FLASHCARD_ID))
        assertEquals(emptyList(), enrichment.markedPending)
    }

    @Test
    fun `exhausted credits are retried once the reset has passed`() = runTest {
        val enrichment = RecordingEnrichmentRepository()
        val useCase: RetryEnrichmentUseCase = useCaseFor(
            EnrichmentFailureCause.CreditsExhausted,
            enrichment,
            credits = GenerationCredits(remaining = 0, resetAt = NOW.minusSeconds(60)),
        )

        assertTrue(useCase(FLASHCARD_ID))
        assertEquals(listOf(listOf(FLASHCARD_ID)), enrichment.markedPending)
    }

    @Test
    fun `a card that has not failed is not retried`() = runTest {
        val enrichment = RecordingEnrichmentRepository()
        val useCase: RetryEnrichmentUseCase = useCaseFor(cause = null, enrichment = enrichment)

        assertFalse(useCase(FLASHCARD_ID))
        assertEquals(emptyList(), enrichment.markedPending)
    }

    private fun useCaseFor(
        cause: EnrichmentFailureCause?,
        enrichment: RecordingEnrichmentRepository,
        credits: GenerationCredits? = null,
    ): RetryEnrichmentUseCase = RetryEnrichmentUseCase(
        flashcardRepository = SingleCardRepository(cause),
        enrichmentRepository = enrichment,
        creditsRepository = FixedCreditsRepository(credits),
        clock = Clock { NOW },
    )

    private companion object {
        val FLASHCARD_ID: FlashcardId = "card-1".toFlashcardId()
        val NOW: Instant = Instant.parse("2026-09-10T20:00:00Z")
    }
}

private class RecordingEnrichmentRepository : FlashcardEnrichmentRepository {

    val markedPending: MutableList<List<FlashcardId>> = mutableListOf()

    override suspend fun findIdsByStatus(status: EnrichmentStatus): List<FlashcardId> = emptyList()

    override suspend fun markPending(ids: List<FlashcardId>) {
        markedPending += ids
    }
}

private class FixedCreditsRepository(private val credits: GenerationCredits?) : GenerationCreditsRepository {

    override fun observe(): Flow<GenerationCredits?> = flowOf(credits)

    override suspend fun record(credits: GenerationCredits) = Unit
}

private class SingleCardRepository(private val cause: EnrichmentFailureCause?) : FlashcardRepository {

    override suspend fun fetchById(id: FlashcardId): FlashcardDetail {
        val status: EnrichmentStatus = if (cause == null) EnrichmentStatus.ENRICHED else EnrichmentStatus.FAILED
        val flashcard: Flashcard = Flashcard.empty(SystemClock).copy(
            id = id,
            enrichmentStatus = status,
            enrichmentFailureCause = cause,
        )
        return FlashcardDetail(flashcard = flashcard)
    }

    override fun observeById(id: FlashcardId): Flow<FlashcardDetail?> = throw UnsupportedOperationException()
    override fun fetchAll() = throw UnsupportedOperationException()
    override fun fetchByDeckId(deckId: DeckId) = throw UnsupportedOperationException()
    override suspend fun create(input: CreateFlashcardInput): FlashcardId = throw UnsupportedOperationException()
    override suspend fun update(input: UpdateFlashcardInput) = throw UnsupportedOperationException()
    override suspend fun updateEnrichmentStatus(
        flashcardId: FlashcardId,
        status: EnrichmentStatus,
        failure: EnrichmentFailure?,
    ) = throw UnsupportedOperationException()
    override suspend fun recordPromptVersion(flashcardId: FlashcardId, promptVersion: Int) = Unit
    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = 0L
    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = Unit
    override suspend fun countDueFlashcards(nowMillis: Long): Long = 0L
    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) = Unit
    override suspend fun fetchRecentWords(limit: Int): List<String> = emptyList()
}

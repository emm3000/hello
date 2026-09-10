package com.emm.domain.authoring

import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.generation.GenerationRefusalCode
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkEnrichmentFailedUseCaseTest {

    @Test
    fun `invoke stores the failed status and reason for the given card`() = runTest {
        val repository = StatusRecordingRepository()
        val useCase = MarkEnrichmentFailedUseCase(repository)

        useCase(FLASHCARD_ID, EnrichmentFailure(null, "No pude entender esa entrada."))

        val expected: List<Triple<FlashcardId, EnrichmentStatus, EnrichmentFailure?>> = listOf(
            Triple(
                FLASHCARD_ID,
                EnrichmentStatus.FAILED,
                EnrichmentFailure(null, "No pude entender esa entrada."),
            ),
        )
        assertEquals(expected, repository.written)
    }

    @Test
    fun `invoke stores the typed refusal code alongside the reason`() = runTest {
        val repository = StatusRecordingRepository()
        val useCase = MarkEnrichmentFailedUseCase(repository)

        useCase(
            FLASHCARD_ID,
            EnrichmentFailure(GenerationRefusalCode.Unintelligible, "No pude entender esa entrada."),
        )

        val expected: List<Triple<FlashcardId, EnrichmentStatus, EnrichmentFailure?>> = listOf(
            Triple(
                FLASHCARD_ID,
                EnrichmentStatus.FAILED,
                EnrichmentFailure(GenerationRefusalCode.Unintelligible, "No pude entender esa entrada."),
            ),
        )
        assertEquals(expected, repository.written)
    }

    @Test
    fun `invoke stores the failed status with no failure when nothing is known`() = runTest {
        val repository = StatusRecordingRepository()
        val useCase = MarkEnrichmentFailedUseCase(repository)

        useCase(FLASHCARD_ID, null)

        val expected: List<Triple<FlashcardId, EnrichmentStatus, EnrichmentFailure?>> =
            listOf(Triple(FLASHCARD_ID, EnrichmentStatus.FAILED, null))
        assertEquals(expected, repository.written)
    }

    private companion object {
        val FLASHCARD_ID: FlashcardId = "flashcard-1".toFlashcardId()
    }
}

private class StatusRecordingRepository : FlashcardRepository {

    val written: MutableList<Triple<FlashcardId, EnrichmentStatus, EnrichmentFailure?>> = mutableListOf()

    override suspend fun updateEnrichmentStatus(
        flashcardId: FlashcardId,
        status: EnrichmentStatus,
        failure: EnrichmentFailure?,
    ) {
        written += Triple(flashcardId, status, failure)
    }

    override fun fetchAll() = throw UnsupportedOperationException()
    override fun fetchByDeckId(deckId: DeckId) = throw UnsupportedOperationException()
    override suspend fun fetchById(id: FlashcardId): FlashcardDetail = throw UnsupportedOperationException()
    override suspend fun create(input: CreateFlashcardInput): FlashcardId = throw UnsupportedOperationException()
    override suspend fun update(input: UpdateFlashcardInput) = throw UnsupportedOperationException()
    override suspend fun recordPromptVersion(flashcardId: FlashcardId, promptVersion: Int) = Unit
    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = 0L
    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = Unit
    override suspend fun countDueFlashcards(nowMillis: Long): Long = 0L
    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) = Unit
    override suspend fun fetchRecentWords(limit: Int): List<String> = emptyList()
}

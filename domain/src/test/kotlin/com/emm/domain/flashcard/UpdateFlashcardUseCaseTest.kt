package com.emm.domain.flashcard

import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UpdateFlashcardUseCaseTest {

    @Test
    fun `invoke rejects a blank word and never reaches the repository`() = runTest {
        val repository = UpdateRecordingRepository()
        val useCase = UpdateFlashcardUseCase(repository)

        assertFailsWith<IllegalArgumentException> {
            useCase(input(word = "   "))
        }

        assertTrue(repository.updated.isEmpty())
    }

    @Test
    fun `invoke accepts a blank meaning so an unenriched card can be edited`() = runTest {
        val repository = UpdateRecordingRepository()
        val useCase = UpdateFlashcardUseCase(repository)

        useCase(input(word = "lantern", meaning = ""))

        assertEquals(1, repository.updated.size)
        assertEquals("lantern", repository.updated.first().word)
    }

    @Test
    fun `invoke passes the input through to the repository`() = runTest {
        val repository = UpdateRecordingRepository()
        val useCase = UpdateFlashcardUseCase(repository)
        val expected = input(word = "lantern", meaning = "a lamp you can carry")

        useCase(expected)

        assertEquals(listOf(expected), repository.updated)
    }

    @Test
    fun `invoke marks a FAILED card ENRICHED when it is saved with a meaning`() = runTest {
        val repository = UpdateRecordingRepository(status = EnrichmentStatus.FAILED)
        val useCase = UpdateFlashcardUseCase(repository)

        useCase(input(word = "lantern", meaning = "a definition"))

        assertEquals(listOf(FLASHCARD_ID to EnrichmentStatus.ENRICHED), repository.statusUpdates)
    }

    @Test
    fun `invoke leaves a FAILED card FAILED when it is saved with a blank meaning`() = runTest {
        val repository = UpdateRecordingRepository(status = EnrichmentStatus.FAILED)
        val useCase = UpdateFlashcardUseCase(repository)

        useCase(input(word = "lantern", meaning = ""))

        assertTrue(repository.statusUpdates.isEmpty())
        assertEquals(0, repository.fetches)
    }

    @Test
    fun `invoke leaves a PENDING card PENDING when it is saved with a meaning`() = runTest {
        val repository = UpdateRecordingRepository(status = EnrichmentStatus.PENDING)
        val useCase = UpdateFlashcardUseCase(repository)

        useCase(input(word = "lantern", meaning = "a definition"))

        assertTrue(repository.statusUpdates.isEmpty())
    }

    @Test
    fun `invoke never rewrites the status of an ENRICHED card`() = runTest {
        val repository = UpdateRecordingRepository(status = EnrichmentStatus.ENRICHED)
        val useCase = UpdateFlashcardUseCase(repository)

        useCase(input(word = "lantern", meaning = "a definition"))

        assertTrue(repository.statusUpdates.isEmpty())
    }

    @Test
    fun `invoke saves the fields before it marks the card ENRICHED`() = runTest {
        val repository = UpdateRecordingRepository(status = EnrichmentStatus.FAILED)
        val useCase = UpdateFlashcardUseCase(repository)

        useCase(input(word = "lantern", meaning = "a definition"))

        assertEquals(listOf("update", "status"), repository.calls)
    }

    private fun input(word: String, meaning: String = ""): UpdateFlashcardInput {
        return UpdateFlashcardInput(
            flashcardId = FLASHCARD_ID,
            word = word,
            meaning = meaning,
            translation = "farol",
        )
    }

    private companion object {
        val FLASHCARD_ID: FlashcardId = "flashcard-1".toFlashcardId()
    }
}

private class UpdateRecordingRepository(
    private val status: EnrichmentStatus = EnrichmentStatus.ENRICHED,
) : FlashcardRepository {

    val updated: MutableList<UpdateFlashcardInput> = mutableListOf()
    val statusUpdates: MutableList<Pair<FlashcardId, EnrichmentStatus>> = mutableListOf()
    val calls: MutableList<String> = mutableListOf()
    var fetches: Int = 0

    override suspend fun update(input: UpdateFlashcardInput) {
        updated += input
        calls += "update"
    }

    override fun fetchAll(): Flow<List<Flashcard>> = error("unused")

    override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> = error("unused")

    override suspend fun fetchById(id: FlashcardId): FlashcardDetail {
        fetches += 1
        return FlashcardDetail(flashcard = Flashcard.empty(SystemClock).copy(id = id, enrichmentStatus = status))
    }

    override suspend fun create(input: CreateFlashcardInput): FlashcardId = error("unused")

    override suspend fun updateEnrichmentStatus(
        flashcardId: FlashcardId,
        status: EnrichmentStatus,
        failureReason: String?,
    ) {
        statusUpdates += flashcardId to status
        calls += "status"
    }

    override suspend fun recordPromptVersion(flashcardId: FlashcardId, promptVersion: Int) = error("unused")

    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = error("unused")

    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = error("unused")

    override suspend fun countDueFlashcards(nowMillis: Long): Long = error("unused")

    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) = error("unused")

    override suspend fun fetchRecentWords(limit: Int): List<String> = error("unused")
}

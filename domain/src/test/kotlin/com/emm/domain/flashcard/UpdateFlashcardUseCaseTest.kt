package com.emm.domain.flashcard

import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
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

private class UpdateRecordingRepository : FlashcardRepository {

    val updated: MutableList<UpdateFlashcardInput> = mutableListOf()

    override suspend fun update(input: UpdateFlashcardInput) {
        updated += input
    }

    override fun fetchAll(): Flow<List<Flashcard>> = error("unused")

    override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> = error("unused")

    override suspend fun fetchById(id: FlashcardId): FlashcardDetail = error("unused")

    override suspend fun create(input: CreateFlashcardInput): FlashcardId = error("unused")

    override suspend fun updateEnrichmentStatus(flashcardId: FlashcardId, status: EnrichmentStatus) =
        error("unused")

    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = error("unused")

    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = error("unused")

    override suspend fun countDueFlashcards(nowMillis: Long): Long = error("unused")

    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) = error("unused")
}

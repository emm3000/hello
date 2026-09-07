package com.emm.domain.authoring

import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.ExactDuplicateKey
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Expression
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateManualFlashcardUseCaseTest {

    @Test
    fun `invoke creates an enriched flashcard with the written translation`() = runTest {
        val repository = ManualFlashcardRepository()
        val useCase = CreateManualFlashcardUseCase(
            repository = repository,
            duplicateRepository = ManualDuplicateRepository(exists = false),
        )

        val flashcardId: FlashcardId = useCase(
            deckId = "deck-1".toDeckId(),
            word = "  give   up  ",
            translation = "  rendirse  ",
            meaning = "  to stop trying  ",
        )

        val input: CreateFlashcardInput = repository.requireLastInput()
        assertEquals("flashcard-1".toFlashcardId(), flashcardId)
        assertEquals(EnrichmentStatus.ENRICHED, input.enrichmentStatus)
        assertEquals("deck-1".toDeckId(), input.deckId)
        assertEquals("give up", input.word)
        assertEquals("rendirse", input.translation)
        assertEquals("to stop trying", input.meaning)
        assertEquals("", input.phonetic)
    }

    @Test
    fun `invoke accepts a blank meaning`() = runTest {
        val repository = ManualFlashcardRepository()
        val useCase = CreateManualFlashcardUseCase(
            repository = repository,
            duplicateRepository = ManualDuplicateRepository(exists = false),
        )

        useCase(deckId = "deck-1".toDeckId(), word = "borrow", translation = "prestar")

        val input: CreateFlashcardInput = repository.requireLastInput()
        assertEquals("", input.meaning)
        assertEquals(EnrichmentStatus.ENRICHED, input.enrichmentStatus)
    }

    @Test
    fun `invoke rejects a blank word`() = runTest {
        val repository = ManualFlashcardRepository()
        val useCase = CreateManualFlashcardUseCase(
            repository = repository,
            duplicateRepository = ManualDuplicateRepository(exists = false),
        )

        val error: DomainValidationException = assertFailsWith {
            useCase(deckId = "deck-1".toDeckId(), word = "   ", translation = "rendirse")
        }

        assertTrue(error.issues.any { it.code == IssueCode.EmptyUserText && it.field == "word" })
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `invoke rejects a blank translation`() = runTest {
        val repository = ManualFlashcardRepository()
        val useCase = CreateManualFlashcardUseCase(
            repository = repository,
            duplicateRepository = ManualDuplicateRepository(exists = false),
        )

        val error: DomainValidationException = assertFailsWith {
            useCase(deckId = "deck-1".toDeckId(), word = "give up", translation = "   ")
        }

        assertTrue(error.issues.any { it.code == IssueCode.EmptyTranslation && it.field == "translation" })
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `invoke rejects a word already present in the deck`() = runTest {
        val repository = ManualFlashcardRepository()
        val useCase = CreateManualFlashcardUseCase(
            repository = repository,
            duplicateRepository = ManualDuplicateRepository(exists = true),
        )

        val error: DomainValidationException = assertFailsWith {
            useCase(deckId = "deck-1".toDeckId(), word = "borrow", translation = "prestar")
        }

        assertTrue(error.issues.any { it.code == IssueCode.DuplicateWordInDeck && it.field == "word" })
        assertEquals(0, repository.createCalls)
    }
}

private class ManualFlashcardRepository : FlashcardRepository {

    var createCalls: Int = 0
    private var lastInput: CreateFlashcardInput? = null

    fun requireLastInput(): CreateFlashcardInput = requireNotNull(lastInput)

    override suspend fun create(input: CreateFlashcardInput): FlashcardId {
        createCalls += 1
        lastInput = input
        return "flashcard-1".toFlashcardId()
    }

    override fun fetchAll() = throw UnsupportedOperationException()
    override fun fetchByDeckId(deckId: DeckId) = throw UnsupportedOperationException()

    override suspend fun fetchById(id: FlashcardId): FlashcardDetail {
        return FlashcardDetail(flashcard = Flashcard.empty(SystemClock).copy(id = id))
    }

    override suspend fun update(input: UpdateFlashcardInput) = Unit

    override suspend fun updateEnrichmentStatus(
        flashcardId: FlashcardId,
        status: EnrichmentStatus,
        failureReason: String?,
    ) = Unit
    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = 0L
    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = Unit
    override suspend fun countDueFlashcards(nowMillis: Long): Long = 0L
    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) = Unit
    override suspend fun fetchRecentWords(limit: Int): List<String> = emptyList()
}

private class ManualDuplicateRepository(
    private val exists: Boolean,
) : FlashcardDuplicateRepository {

    override suspend fun existsExactDuplicate(key: ExactDuplicateKey): Boolean = false

    override suspend fun existsExpressionInDeck(deckId: DeckId, expression: Expression): Boolean = exists
}

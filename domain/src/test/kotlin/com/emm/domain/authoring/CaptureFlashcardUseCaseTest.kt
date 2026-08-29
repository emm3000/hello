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
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class CaptureFlashcardUseCaseTest {

    @Test
    fun `invoke creates a pending flashcard from a bare word`() = runTest {
        val repository = RecordingFlashcardRepository()
        val useCase = CaptureFlashcardUseCase(
            repository = repository,
            duplicateRepository = ExpressionDuplicateRepository(exists = false),
        )

        val flashcardId: FlashcardId = useCase(deckId = "deck-1".toDeckId(), word = "borrow")

        val input: CreateFlashcardInput = repository.requireLastInput()
        assertEquals("flashcard-1".toFlashcardId(), flashcardId)
        assertEquals(EnrichmentStatus.PENDING, input.enrichmentStatus)
        assertEquals("deck-1".toDeckId(), input.deckId)
        assertEquals("borrow", input.word)
        assertEquals("", input.meaning)
        assertEquals("", input.translation)
        assertEquals("", input.phonetic)
    }

    @Test
    fun `invoke normalizes the captured word`() = runTest {
        val repository = RecordingFlashcardRepository()
        val useCase = CaptureFlashcardUseCase(
            repository = repository,
            duplicateRepository = ExpressionDuplicateRepository(exists = false),
        )

        useCase(deckId = "deck-1".toDeckId(), word = "  give   up  ")

        assertEquals("give up", repository.requireLastInput().word)
    }

    @Test
    fun `invoke checks uniqueness with the normalized expression`() = runTest {
        val duplicateRepository = ExpressionDuplicateRepository(exists = false)
        val useCase = CaptureFlashcardUseCase(
            repository = RecordingFlashcardRepository(),
            duplicateRepository = duplicateRepository,
        )

        useCase(deckId = "deck-1".toDeckId(), word = "  Borrow ")

        assertEquals("deck-1".toDeckId(), duplicateRepository.lastDeckId)
        assertEquals("borrow", duplicateRepository.lastExpression?.canonical)
    }

    @Test
    fun `invoke rejects a blank word`() = runTest {
        val repository = RecordingFlashcardRepository()
        val useCase = CaptureFlashcardUseCase(
            repository = repository,
            duplicateRepository = ExpressionDuplicateRepository(exists = false),
        )

        val error: DomainValidationException = assertFailsWith {
            useCase(deckId = "deck-1".toDeckId(), word = "   ")
        }

        assertTrue(error.issues.any { it.code == IssueCode.EmptyUserText })
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `invoke rejects a word already present in the deck`() = runTest {
        val repository = RecordingFlashcardRepository()
        val useCase = CaptureFlashcardUseCase(
            repository = repository,
            duplicateRepository = ExpressionDuplicateRepository(exists = true),
        )

        val error: DomainValidationException = assertFailsWith {
            useCase(deckId = "deck-1".toDeckId(), word = "borrow")
        }

        assertTrue(error.issues.any { it.code == IssueCode.DuplicateWordInDeck })
        assertEquals(0, repository.createCalls)
    }
}

private class RecordingFlashcardRepository : FlashcardRepository {

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

    override suspend fun updateEnrichmentStatus(flashcardId: FlashcardId, status: EnrichmentStatus) = Unit
    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = 0L
    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = Unit
    override suspend fun countDueFlashcards(nowMillis: Long): Long = 0L
    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) = Unit
    override suspend fun fetchRecentWords(limit: Int): List<String> = emptyList()
}

private class ExpressionDuplicateRepository(
    private val exists: Boolean,
) : FlashcardDuplicateRepository {

    var lastDeckId: DeckId? = null
    var lastExpression: Expression? = null

    override suspend fun existsExactDuplicate(key: ExactDuplicateKey): Boolean = false

    override suspend fun existsExpressionInDeck(deckId: DeckId, expression: Expression): Boolean {
        lastDeckId = deckId
        lastExpression = expression
        return exists
    }
}

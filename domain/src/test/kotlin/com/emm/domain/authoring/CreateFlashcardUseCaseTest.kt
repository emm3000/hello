package com.emm.domain.authoring

import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.ExactDuplicateKey
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.flashcard.Expression
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateFlashcardUseCaseTest {

    @Test
    fun `invoke rejects invalid learning note`() = runTest {
        val useCase = CreateFlashcardUseCase(
            repository = FakeFlashcardRepository(),
            validateGeneratedLearningNoteUseCase = ValidateGeneratedLearningNoteUseCase(),
            generatedLearningNoteMapper = GeneratedLearningNoteMapper(),
            ensureUniqueFlashcardInDeckUseCase = EnsureUniqueFlashcardInDeckUseCase(
                isExactDuplicateGeneratedNoteUseCase = IsExactDuplicateGeneratedNoteUseCase(
                    repository = DuplicateRepoStub(exists = false),
                ),
            ),
        )

        val error = assertFailsWith<com.emm.domain.validation.DomainValidationException> {
            useCase(
                deckId = "deck-1".toDeckId(),
                learningNote = sampleWordNote().copy(cards = emptyList()),
            )
        }

        assertTrue(error.issues.isNotEmpty())
    }

    @Test
    fun `invoke persists valid learning note`() = runTest {
        val repository = FakeFlashcardRepository()
        val useCase = CreateFlashcardUseCase(
            repository = repository,
            validateGeneratedLearningNoteUseCase = ValidateGeneratedLearningNoteUseCase(),
            generatedLearningNoteMapper = GeneratedLearningNoteMapper(),
            ensureUniqueFlashcardInDeckUseCase = EnsureUniqueFlashcardInDeckUseCase(
                isExactDuplicateGeneratedNoteUseCase = IsExactDuplicateGeneratedNoteUseCase(
                    repository = DuplicateRepoStub(exists = false),
                ),
            ),
        )

        useCase(deckId = "deck-1".toDeckId(), learningNote = sampleWordNote())

        assertEquals(1, repository.createCalls)
        assertEquals(1, repository.upsertExamplesCalls)
    }

    @Test
    fun `invoke rejects exact duplicate in deck`() = runTest {
        val useCase = CreateFlashcardUseCase(
            repository = FakeFlashcardRepository(),
            validateGeneratedLearningNoteUseCase = ValidateGeneratedLearningNoteUseCase(),
            generatedLearningNoteMapper = GeneratedLearningNoteMapper(),
            ensureUniqueFlashcardInDeckUseCase = EnsureUniqueFlashcardInDeckUseCase(
                isExactDuplicateGeneratedNoteUseCase = IsExactDuplicateGeneratedNoteUseCase(
                    repository = DuplicateRepoStub(exists = true),
                ),
            ),
        )

        val error = assertFailsWith<com.emm.domain.validation.DomainValidationException> {
            useCase(deckId = "deck-1".toDeckId(), learningNote = sampleWordNote())
        }

        assertTrue(error.issues.any { it.code == com.emm.domain.validation.IssueCode.DuplicateExactCardInDeck })
    }
}

private class FakeFlashcardRepository : FlashcardRepository {
    var createCalls = 0
    var upsertExamplesCalls = 0

    override fun fetchAll() = throw UnsupportedOperationException()
    override fun fetchByDeckId(deckId: DeckId) = throw UnsupportedOperationException()

    override suspend fun create(input: CreateFlashcardInput): FlashcardId {
        createCalls += 1
        return "flashcard-1".toFlashcardId()
    }

    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) {
        upsertExamplesCalls += 1
    }

    override suspend fun fetchById(id: FlashcardId): FlashcardDetail {
        return FlashcardDetail(flashcard = Flashcard.empty(SystemClock).copy(id = id))
    }

    override suspend fun update(input: UpdateFlashcardInput) = Unit

    override suspend fun updateEnrichmentStatus(flashcardId: FlashcardId, status: EnrichmentStatus) = Unit
    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = 0L
    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = Unit
    override suspend fun countDueFlashcards(nowMillis: Long): Long = 0L
    override suspend fun fetchRecentWords(limit: Int): List<String> = emptyList()
}

private class DuplicateRepoStub(
    private val exists: Boolean,
) : FlashcardDuplicateRepository {
    override suspend fun existsExactDuplicate(key: ExactDuplicateKey): Boolean = exists

    override suspend fun existsExpressionInDeck(deckId: DeckId, expression: Expression): Boolean = false
}

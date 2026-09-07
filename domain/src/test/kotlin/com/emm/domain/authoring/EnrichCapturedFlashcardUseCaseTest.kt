package com.emm.domain.authoring

import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardGenerationRepository
import com.emm.domain.flashcard.FlashcardInputType
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GenerationCreditsExhaustedException
import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnrichCapturedFlashcardUseCaseTest {

    @Test
    fun `invoke applies the generated note and marks the card enriched`() = runTest {
        val repository = RecordingRepository()
        val useCase: EnrichCapturedFlashcardUseCase = useCase(
            repository = repository,
            generationRepository = NoteGenerationRepository(outcomes = listOf(Result.success(sampleWordNote()))),
        )

        val status: EnrichmentStatus = useCase(FLASHCARD_ID)

        val applied: UpdateFlashcardInput = requireNotNull(repository.lastUpdate)
        assertEquals(EnrichmentStatus.ENRICHED, status)
        assertEquals(EnrichmentStatus.ENRICHED, repository.lastStatus)
        assertEquals(FLASHCARD_ID, applied.flashcardId)
        assertEquals("borrow", applied.word)
        assertEquals("pedir prestado", applied.translation)
        assertEquals(listOf("Can I borrow your pen?"), repository.lastExamples.map { it.text })
    }

    @Test
    fun `invoke generates from the captured word`() = runTest {
        val generationRepository = NoteGenerationRepository(outcomes = listOf(Result.success(sampleWordNote())))
        val useCase: EnrichCapturedFlashcardUseCase = useCase(
            repository = RecordingRepository(),
            generationRepository = generationRepository,
        )

        useCase(FLASHCARD_ID)

        val input: FlashcardGenerationInput = requireNotNull(generationRepository.inputs.firstOrNull())
        assertEquals("borrow", input.userText)
        assertEquals(FlashcardInputType.Word, input.inputType)
    }

    @Test
    fun `invoke propagates the generation error without storing anything`() = runTest {
        val repository = RecordingRepository()
        val generationRepository = NoteGenerationRepository(
            outcomes = listOf(Result.failure(IllegalStateException("boom"))),
        )
        val useCase: EnrichCapturedFlashcardUseCase = useCase(
            repository = repository,
            generationRepository = generationRepository,
        )

        assertFailsWith<IllegalStateException> { useCase(FLASHCARD_ID) }

        assertNull(repository.lastStatus)
        assertNull(repository.lastUpdate)
        assertEquals(1, generationRepository.inputs.size)
    }

    @Test
    fun `invoke propagates the credits error without storing anything`() = runTest {
        val repository = RecordingRepository()
        val creditsError = GenerationCreditsExhaustedException(resetAt = Instant.EPOCH)
        val useCase: EnrichCapturedFlashcardUseCase = useCase(
            repository = repository,
            generationRepository = NoteGenerationRepository(outcomes = listOf(Result.failure(creditsError))),
        )

        assertFailsWith<GenerationCreditsExhaustedException> { useCase(FLASHCARD_ID) }

        assertNull(repository.lastStatus)
        assertNull(repository.lastUpdate)
    }

    @Test
    fun `invoke regenerates once when the first note fails validation and the second note is valid`() = runTest {
        val repository = RecordingRepository()
        val invalidNote = sampleWordNote().copy(cards = emptyList())
        val generationRepository = NoteGenerationRepository(
            outcomes = listOf(Result.success(invalidNote), Result.success(sampleWordNote())),
        )
        val useCase: EnrichCapturedFlashcardUseCase = useCase(
            repository = repository,
            generationRepository = generationRepository,
        )

        val status: EnrichmentStatus = useCase(FLASHCARD_ID)

        assertEquals(EnrichmentStatus.ENRICHED, status)
        assertEquals(2, generationRepository.inputs.size)
        assertTrue(generationRepository.inputs[0].previousIssues.isEmpty())
        val expectedIssues = ValidateGeneratedLearningNoteUseCase()(invalidNote).errors
        assertEquals(expectedIssues, generationRepository.inputs[1].previousIssues)
        assertEquals("borrow", generationRepository.inputs[1].userText)
    }

    @Test
    fun `invoke regenerates once when the repository throws a validation error and the second attempt succeeds`() =
        runTest {
            val repository = RecordingRepository()
            val issues = listOf(ValidationIssue.Error(IssueCode.MissingUsagePattern, "usage_pattern"))
            val generationRepository = NoteGenerationRepository(
                outcomes = listOf(
                    Result.failure(DomainValidationException(issues)),
                    Result.success(sampleWordNote()),
                ),
            )
            val useCase: EnrichCapturedFlashcardUseCase = useCase(
                repository = repository,
                generationRepository = generationRepository,
            )

            val status: EnrichmentStatus = useCase(FLASHCARD_ID)

            assertEquals(EnrichmentStatus.ENRICHED, status)
            assertEquals(2, generationRepository.inputs.size)
            assertEquals(issues, generationRepository.inputs[1].previousIssues)
        }

    @Test
    fun `invoke propagates the second validation error without storing anything when both attempts fail`() =
        runTest {
            val repository = RecordingRepository()
            val invalidNote = sampleWordNote().copy(cards = emptyList())
            val generationRepository = NoteGenerationRepository(
                outcomes = listOf(Result.success(invalidNote), Result.success(invalidNote)),
            )
            val useCase: EnrichCapturedFlashcardUseCase = useCase(
                repository = repository,
                generationRepository = generationRepository,
            )

            assertFailsWith<DomainValidationException> { useCase(FLASHCARD_ID) }

            assertEquals(2, generationRepository.inputs.size)
            assertNull(repository.lastStatus)
            assertNull(repository.lastUpdate)
        }

    private fun useCase(
        repository: FlashcardRepository,
        generationRepository: FlashcardGenerationRepository,
    ): EnrichCapturedFlashcardUseCase {
        return EnrichCapturedFlashcardUseCase(
            repository = repository,
            generationRepository = generationRepository,
            validateGeneratedLearningNoteUseCase = ValidateGeneratedLearningNoteUseCase(),
            generatedLearningNoteMapper = GeneratedLearningNoteMapper(),
        )
    }

    private companion object {
        val FLASHCARD_ID: FlashcardId = "flashcard-1".toFlashcardId()
    }
}

private class RecordingRepository : FlashcardRepository {

    var lastUpdate: UpdateFlashcardInput? = null
    var lastStatus: EnrichmentStatus? = null
    var lastExamples: List<Example> = emptyList()

    override suspend fun fetchById(id: FlashcardId): FlashcardDetail {
        return FlashcardDetail(flashcard = Flashcard.empty(SystemClock).copy(id = id, word = "borrow"))
    }

    override suspend fun update(input: UpdateFlashcardInput) {
        lastUpdate = input
    }

    override suspend fun updateEnrichmentStatus(
        flashcardId: FlashcardId,
        status: EnrichmentStatus,
        failureReason: String?,
    ) {
        lastStatus = status
    }

    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) {
        lastExamples = examples
    }

    override fun fetchAll() = throw UnsupportedOperationException()
    override fun fetchByDeckId(deckId: DeckId) = throw UnsupportedOperationException()
    override suspend fun create(input: CreateFlashcardInput): FlashcardId = throw UnsupportedOperationException()
    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = 0L
    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = Unit
    override suspend fun countDueFlashcards(nowMillis: Long): Long = 0L
    override suspend fun fetchRecentWords(limit: Int): List<String> = emptyList()
}

private class NoteGenerationRepository(
    private val outcomes: List<Result<GeneratedLearningNote>>,
) : FlashcardGenerationRepository {

    val inputs: MutableList<FlashcardGenerationInput> = mutableListOf()
    private var callIndex: Int = 0

    override suspend fun generateLearningNote(input: FlashcardGenerationInput): GeneratedLearningNote {
        inputs += input
        val outcome = outcomes[callIndex]
        callIndex += 1
        return outcome.getOrThrow()
    }
}

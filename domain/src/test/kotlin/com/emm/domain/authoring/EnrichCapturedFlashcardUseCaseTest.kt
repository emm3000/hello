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
import com.emm.domain.generation.GeneratedExampleDraft
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.GenerationQuotaExceededException
import com.emm.domain.generation.RegenerableNoteField
import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import com.emm.domain.validation.DomainValidationException
import kotlinx.coroutines.test.runTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class EnrichCapturedFlashcardUseCaseTest {

    @Test
    fun `invoke applies the generated note and marks the card enriched`() = runTest {
        val repository = RecordingRepository()
        val useCase: EnrichCapturedFlashcardUseCase = useCase(
            repository = repository,
            generationRepository = NoteGenerationRepository(note = sampleWordNote()),
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
        val generationRepository = NoteGenerationRepository(note = sampleWordNote())
        val useCase: EnrichCapturedFlashcardUseCase = useCase(
            repository = RecordingRepository(),
            generationRepository = generationRepository,
        )

        useCase(FLASHCARD_ID)

        val input: FlashcardGenerationInput = requireNotNull(generationRepository.lastInput)
        assertEquals("borrow", input.userText)
        assertEquals(FlashcardInputType.Word, input.inputType)
    }

    @Test
    fun `invoke propagates the generation error without storing anything`() = runTest {
        val repository = RecordingRepository()
        val useCase: EnrichCapturedFlashcardUseCase = useCase(
            repository = repository,
            generationRepository = NoteGenerationRepository(error = IllegalStateException("boom")),
        )

        assertFailsWith<IllegalStateException> { useCase(FLASHCARD_ID) }

        assertNull(repository.lastStatus)
        assertNull(repository.lastUpdate)
    }

    @Test
    fun `invoke propagates the quota error without storing anything`() = runTest {
        val repository = RecordingRepository()
        val quotaError = GenerationQuotaExceededException(limit = 50, resetAt = Instant.EPOCH)
        val useCase: EnrichCapturedFlashcardUseCase = useCase(
            repository = repository,
            generationRepository = NoteGenerationRepository(error = quotaError),
        )

        assertFailsWith<GenerationQuotaExceededException> { useCase(FLASHCARD_ID) }

        assertNull(repository.lastStatus)
        assertNull(repository.lastUpdate)
    }

    @Test
    fun `invoke propagates the validation error without storing anything`() = runTest {
        val repository = RecordingRepository()
        val useCase: EnrichCapturedFlashcardUseCase = useCase(
            repository = repository,
            generationRepository = NoteGenerationRepository(note = sampleWordNote().copy(cards = emptyList())),
        )

        assertFailsWith<DomainValidationException> { useCase(FLASHCARD_ID) }

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

    override suspend fun updateEnrichmentStatus(flashcardId: FlashcardId, status: EnrichmentStatus) {
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
    private val note: GeneratedLearningNote? = null,
    private val error: Throwable? = null,
) : FlashcardGenerationRepository {

    var lastInput: FlashcardGenerationInput? = null

    override suspend fun generateLearningNote(input: FlashcardGenerationInput): GeneratedLearningNote {
        lastInput = input
        error?.let { throw it }
        return requireNotNull(note)
    }

    override suspend fun regenerateNoteField(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
        field: RegenerableNoteField,
    ): String = throw UnsupportedOperationException()

    override suspend fun regenerateExample(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
    ): GeneratedExampleDraft = throw UnsupportedOperationException()

    override suspend fun regenerateClozeSentence(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
    ): String = throw UnsupportedOperationException()

    override suspend fun regenerateStudyCard(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
        cardId: String,
    ): GeneratedStudyCard = throw UnsupportedOperationException()
}

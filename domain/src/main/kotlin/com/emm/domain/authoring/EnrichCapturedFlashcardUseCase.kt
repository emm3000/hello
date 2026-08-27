package com.emm.domain.authoring

import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardGenerationRepository
import com.emm.domain.flashcard.FlashcardInputType
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.ids.FlashcardId
import com.emm.domain.validation.requireValid
import kotlin.coroutines.cancellation.CancellationException

class EnrichCapturedFlashcardUseCase(
    private val repository: FlashcardRepository,
    private val generationRepository: FlashcardGenerationRepository,
    private val validateGeneratedLearningNoteUseCase: ValidateGeneratedLearningNoteUseCase,
    private val generatedLearningNoteMapper: GeneratedLearningNoteMapper,
) {

    suspend operator fun invoke(flashcardId: FlashcardId): EnrichmentStatus {
        val note: GeneratedLearningNote = generatedNoteOrNull(flashcardId) ?: return markFailed(flashcardId)

        repository.update(
            generatedLearningNoteMapper.toUpdateFlashcardInput(flashcardId = flashcardId, note = note),
        )
        repository.upsertExamples(generatedLearningNoteMapper.toExamples(note), flashcardId)
        repository.updateEnrichmentStatus(flashcardId, EnrichmentStatus.ENRICHED)

        return EnrichmentStatus.ENRICHED
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private suspend fun generatedNoteOrNull(flashcardId: FlashcardId): GeneratedLearningNote? {
        return try {
            val word: String = repository.fetchById(flashcardId).flashcard.word
            val note: GeneratedLearningNote = generationRepository.generateLearningNote(generationInput(word))
            validateGeneratedLearningNoteUseCase(note).requireValid()
            note
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            null
        }
    }

    private suspend fun markFailed(flashcardId: FlashcardId): EnrichmentStatus {
        repository.updateEnrichmentStatus(flashcardId, EnrichmentStatus.FAILED)
        return EnrichmentStatus.FAILED
    }

    private fun generationInput(word: String): FlashcardGenerationInput {
        return FlashcardGenerationInput(inputType = FlashcardInputType.Word, userText = word)
    }
}

package com.emm.domain.flashcard

import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.validation.requireValid

class GenerateLearningNotePreviewUseCase(
    private val repository: FlashcardGenerationRepository,
    private val validateInputUseCase: ValidateFlashcardGenerationInputUseCase,
    private val validateGeneratedLearningNoteUseCase: ValidateGeneratedLearningNoteUseCase,
) {

    suspend operator fun invoke(input: FlashcardGenerationInput): GeneratedLearningNote {
        val normalizedInput = validateInputUseCase(input).requireValid()

        val note = repository.generateLearningNote(normalizedInput)
        validateGeneratedLearningNoteUseCase(note).requireValid()

        return note
    }
}

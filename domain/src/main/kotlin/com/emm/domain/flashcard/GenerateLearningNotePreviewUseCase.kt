package com.emm.domain.flashcard

import com.emm.domain.validation.DomainValidationException

class GenerateLearningNotePreviewUseCase(
    private val repository: FlashcardGenerationRepository,
    private val validateInputUseCase: ValidateFlashcardGenerationInputUseCase,
    private val validateGeneratedLearningNoteUseCase: ValidateGeneratedLearningNoteUseCase,
) {

    suspend operator fun invoke(input: FlashcardGenerationInput): GeneratedLearningNote {
        val inputValidation = validateInputUseCase(input)
        if (!inputValidation.isValid) {
            throw DomainValidationException(inputValidation.errors)
        }

        val note = repository.generateLearningNote(inputValidation.value)
        val noteValidation = validateGeneratedLearningNoteUseCase(note)
        if (!noteValidation.isValid) {
            throw DomainValidationException(noteValidation.errors)
        }

        return note
    }
}

package com.emm.domain.flashcard

import com.emm.domain.validation.DomainValidationException

class RegenerateLearningNoteFieldUseCase(
    private val repository: FlashcardGenerationRepository,
    private val validateInputUseCase: ValidateFlashcardGenerationInputUseCase,
) {

    suspend operator fun invoke(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
        field: RegenerableNoteField,
    ): String {
        val inputValidation = validateInputUseCase(input)
        if (!inputValidation.isValid) {
            throw DomainValidationException(inputValidation.errors)
        }

        return repository.regenerateNoteField(
            input = inputValidation.value,
            note = note,
            field = field,
        )
    }
}

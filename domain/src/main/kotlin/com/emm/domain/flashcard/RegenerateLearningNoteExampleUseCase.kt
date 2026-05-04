package com.emm.domain.flashcard

import com.emm.domain.validation.DomainValidationException

class RegenerateLearningNoteExampleUseCase(
    private val repository: FlashcardGenerationRepository,
    private val validateInputUseCase: ValidateFlashcardGenerationInputUseCase,
) {

    suspend operator fun invoke(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
    ): GeneratedExampleDraft {
        val inputValidation = validateInputUseCase(input)
        if (!inputValidation.isValid) {
            throw DomainValidationException(inputValidation.errors)
        }

        return repository.regenerateExample(
            input = inputValidation.value,
            note = note,
        )
    }
}

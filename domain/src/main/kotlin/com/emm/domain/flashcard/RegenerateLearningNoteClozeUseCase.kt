package com.emm.domain.flashcard

import com.emm.domain.validation.DomainValidationException

class RegenerateLearningNoteClozeUseCase(
    private val repository: FlashcardGenerationRepository,
    private val validateInputUseCase: ValidateFlashcardGenerationInputUseCase,
) {

    suspend operator fun invoke(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
    ): String {
        val inputValidation = validateInputUseCase(input)
        if (!inputValidation.isValid) {
            throw DomainValidationException(inputValidation.errors)
        }

        return repository.regenerateClozeSentence(
            input = inputValidation.value,
            note = note,
        )
    }
}

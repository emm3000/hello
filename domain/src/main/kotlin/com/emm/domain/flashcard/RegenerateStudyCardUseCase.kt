package com.emm.domain.flashcard

import com.emm.domain.validation.DomainValidationException

class RegenerateStudyCardUseCase(
    private val repository: FlashcardGenerationRepository,
    private val validateInputUseCase: ValidateFlashcardGenerationInputUseCase,
) {

    suspend operator fun invoke(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
        cardId: String,
    ): GeneratedStudyCard {
        val inputValidation = validateInputUseCase(input)
        if (!inputValidation.isValid) {
            throw DomainValidationException(inputValidation.errors)
        }

        return repository.regenerateStudyCard(
            input = inputValidation.value,
            note = note,
            cardId = cardId,
        )
    }
}

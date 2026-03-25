package com.emm.domain.flashcard

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
            val message = inputValidation.errors.firstOrNull()?.message ?: "Input invalido para regenerar card."
            throw IllegalArgumentException(message)
        }

        return repository.regenerateStudyCard(
            input = inputValidation.normalizedInput,
            note = note,
            cardId = cardId,
        )
    }
}

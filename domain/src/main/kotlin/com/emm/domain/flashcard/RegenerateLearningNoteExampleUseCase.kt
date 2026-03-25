package com.emm.domain.flashcard

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
            val message = inputValidation.errors.firstOrNull()?.message ?: "Input invalido para regenerar ejemplo."
            throw IllegalArgumentException(message)
        }

        return repository.regenerateExample(
            input = inputValidation.normalizedInput,
            note = note,
        )
    }
}

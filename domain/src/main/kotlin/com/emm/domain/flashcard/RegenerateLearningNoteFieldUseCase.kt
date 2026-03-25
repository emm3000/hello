package com.emm.domain.flashcard

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
            val message = inputValidation.errors.firstOrNull()?.message ?: "Input invalido para regenerar campo."
            throw IllegalArgumentException(message)
        }

        return repository.regenerateNoteField(
            input = inputValidation.normalizedInput,
            note = note,
            field = field,
        )
    }
}

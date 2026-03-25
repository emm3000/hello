package com.emm.domain.flashcard

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
            val message = inputValidation.errors.firstOrNull()?.message ?: "Input invalido para regenerar cloze."
            throw IllegalArgumentException(message)
        }

        return repository.regenerateClozeSentence(
            input = inputValidation.normalizedInput,
            note = note,
        )
    }
}

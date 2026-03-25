package com.emm.domain.flashcard

class GenerateLearningNotePreviewUseCase(
    private val repository: FlashcardGenerationRepository,
    private val validateInputUseCase: ValidateFlashcardGenerationInputUseCase,
    private val validateGeneratedLearningNoteUseCase: ValidateGeneratedLearningNoteUseCase,
) {

    suspend operator fun invoke(input: FlashcardGenerationInput): GeneratedLearningNote {
        val inputValidation = validateInputUseCase(input)
        if (!inputValidation.isValid) {
            val message = inputValidation.errors.firstOrNull()?.message ?: "Input invalido para generar learning note."
            throw IllegalArgumentException(message)
        }

        val note = repository.generateLearningNote(inputValidation.normalizedInput)
        val noteValidation = validateGeneratedLearningNoteUseCase(note)
        if (!noteValidation.isValid) {
            val message = noteValidation.errors.firstOrNull()?.message ?: "Learning note invalida."
            throw IllegalArgumentException(message)
        }

        return note
    }
}

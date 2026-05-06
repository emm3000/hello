package com.emm.domain.flashcard

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.validation.requireValid

class RegenerateLearningNoteFieldUseCase(
    private val repository: FlashcardGenerationRepository,
    private val validateInputUseCase: ValidateFlashcardGenerationInputUseCase,
) {

    suspend operator fun invoke(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
        field: RegenerableNoteField,
    ): String {
        val normalizedInput = validateInputUseCase(input).requireValid()

        return repository.regenerateNoteField(
            input = normalizedInput,
            note = note,
            field = field,
        )
    }
}

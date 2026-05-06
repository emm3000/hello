package com.emm.domain.flashcard

import com.emm.domain.generation.GeneratedExampleDraft
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.validation.requireValid

class RegenerateLearningNoteExampleUseCase(
    private val repository: FlashcardGenerationRepository,
    private val validateInputUseCase: ValidateFlashcardGenerationInputUseCase,
) {

    suspend operator fun invoke(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
    ): GeneratedExampleDraft {
        val normalizedInput = validateInputUseCase(input).requireValid()

        return repository.regenerateExample(
            input = normalizedInput,
            note = note,
        )
    }
}

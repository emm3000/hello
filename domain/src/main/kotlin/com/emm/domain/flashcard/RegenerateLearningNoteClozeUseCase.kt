package com.emm.domain.flashcard

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.validation.requireValid

class RegenerateLearningNoteClozeUseCase(
    private val repository: FlashcardGenerationRepository,
    private val validateInputUseCase: ValidateFlashcardGenerationInputUseCase,
) {

    suspend operator fun invoke(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
    ): String {
        val normalizedInput = validateInputUseCase(input).requireValid()

        return repository.regenerateClozeSentence(
            input = normalizedInput,
            note = note,
        )
    }
}

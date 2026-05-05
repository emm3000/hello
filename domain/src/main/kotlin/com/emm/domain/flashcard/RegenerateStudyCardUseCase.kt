package com.emm.domain.flashcard

import com.emm.domain.validation.requireValid

class RegenerateStudyCardUseCase(
    private val repository: FlashcardGenerationRepository,
    private val validateInputUseCase: ValidateFlashcardGenerationInputUseCase,
) {

    suspend operator fun invoke(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
        cardId: String,
    ): GeneratedStudyCard {
        val normalizedInput = validateInputUseCase(input).requireValid()

        return repository.regenerateStudyCard(
            input = normalizedInput,
            note = note,
            cardId = cardId,
        )
    }
}

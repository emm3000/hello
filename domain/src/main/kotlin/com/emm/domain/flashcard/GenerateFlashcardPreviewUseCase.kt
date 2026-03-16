package com.emm.domain.flashcard

class GenerateFlashcardPreviewUseCase(
    private val repository: FlashcardGenerationRepository,
) {
    suspend operator fun invoke(
        word: String,
        categories: StaticCategories,
        difficulty: String,
        typeView: TypeView,
    ): FlashcardGenerated {
        return when (typeView) {
            TypeView.WordOrPhase -> repository.generateFlashcard(word)
            TypeView.WithCategories -> repository.generatedFlashcard(categories, difficulty)
        }
    }
}

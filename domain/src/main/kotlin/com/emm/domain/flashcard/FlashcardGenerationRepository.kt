package com.emm.domain.flashcard

interface FlashcardGenerationRepository {
    suspend fun generateFlashcard(word: String): FlashcardGenerated

    suspend fun generatedFlashcard(categories: StaticCategories, difficulty: String): FlashcardGenerated

    suspend fun generateLearningNote(input: FlashcardGenerationInput): GeneratedLearningNote
}

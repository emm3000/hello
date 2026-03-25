package com.emm.domain.flashcard

interface FlashcardGenerationRepository {
    suspend fun generateLearningNote(input: FlashcardGenerationInput): GeneratedLearningNote
}

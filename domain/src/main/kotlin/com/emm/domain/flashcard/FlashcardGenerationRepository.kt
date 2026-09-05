package com.emm.domain.flashcard

import com.emm.domain.generation.GeneratedLearningNote

interface FlashcardGenerationRepository {
    suspend fun generateLearningNote(input: FlashcardGenerationInput): GeneratedLearningNote
}

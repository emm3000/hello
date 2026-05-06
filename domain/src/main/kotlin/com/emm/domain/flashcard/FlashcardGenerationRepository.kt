package com.emm.domain.flashcard

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GeneratedStudyCard

interface FlashcardGenerationRepository {
    suspend fun generateLearningNote(input: FlashcardGenerationInput): GeneratedLearningNote
    suspend fun regenerateNoteField(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
        field: RegenerableNoteField,
    ): String
    suspend fun regenerateExample(input: FlashcardGenerationInput, note: GeneratedLearningNote): GeneratedExampleDraft
    suspend fun regenerateClozeSentence(input: FlashcardGenerationInput, note: GeneratedLearningNote): String
    suspend fun regenerateStudyCard(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
        cardId: String,
    ): GeneratedStudyCard
}

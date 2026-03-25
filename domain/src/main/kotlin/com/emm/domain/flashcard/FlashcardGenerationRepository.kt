package com.emm.domain.flashcard

interface FlashcardGenerationRepository {
    suspend fun generateLearningNote(input: FlashcardGenerationInput): GeneratedLearningNote
    suspend fun regenerateExample(input: FlashcardGenerationInput, note: GeneratedLearningNote): GeneratedExampleDraft
    suspend fun regenerateClozeSentence(input: FlashcardGenerationInput, note: GeneratedLearningNote): String
    suspend fun regenerateStudyCard(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
        cardId: String,
    ): GeneratedStudyCard
}

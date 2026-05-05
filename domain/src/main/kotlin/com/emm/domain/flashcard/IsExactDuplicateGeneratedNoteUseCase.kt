package com.emm.domain.flashcard

class IsExactDuplicateGeneratedNoteUseCase(
    private val repository: FlashcardDuplicateRepository,
) {

    suspend operator fun invoke(deckId: String, note: GeneratedLearningNote): Boolean {
        val key = ExactDuplicateKey.from(
            deckId = deckId,
            expression = note.expression,
            intendedMeaningEs = note.intendedMeaningEs,
            noteType = note.noteType,
        )

        return repository.existsExactDuplicate(key)
    }
}

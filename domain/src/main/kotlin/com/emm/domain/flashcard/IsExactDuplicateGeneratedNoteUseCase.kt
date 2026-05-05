package com.emm.domain.flashcard

import com.emm.domain.ids.DeckId

class IsExactDuplicateGeneratedNoteUseCase(
    private val repository: FlashcardDuplicateRepository,
) {

    suspend operator fun invoke(deckId: DeckId, note: GeneratedLearningNote): Boolean {
        val key = ExactDuplicateKey.from(
            deckId = deckId.value,
            expression = note.expression,
            intendedMeaningEs = note.intendedMeaningEs,
            noteType = note.noteType,
        )

        return repository.existsExactDuplicate(key)
    }
}

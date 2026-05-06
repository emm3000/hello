package com.emm.domain.authoring

import com.emm.domain.flashcard.ExactDuplicateKey
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.flashcard.GeneratedLearningNote
import com.emm.domain.ids.DeckId

class IsExactDuplicateGeneratedNoteUseCase(
    private val repository: FlashcardDuplicateRepository,
) {

    suspend operator fun invoke(deckId: DeckId, note: GeneratedLearningNote): Boolean {
        val key = ExactDuplicateKey.from(
            deckId = deckId,
            expression = note.requireExpression(),
            intendedMeaningEs = note.requireIntendedMeaningEs(),
            noteType = note.noteType,
        )

        return repository.existsExactDuplicate(key)
    }
}

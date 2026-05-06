package com.emm.domain.authoring

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.validation.requireValid

class CreateFlashcardUseCase(
    private val repository: FlashcardRepository,
    private val validateGeneratedLearningNoteUseCase: ValidateGeneratedLearningNoteUseCase,
    private val ensureUniqueFlashcardInDeckUseCase: EnsureUniqueFlashcardInDeckUseCase,
    private val generatedLearningNoteMapper: GeneratedLearningNoteMapper = GeneratedLearningNoteMapper(),
) {

    suspend operator fun invoke(
        deckId: DeckId,
        learningNote: GeneratedLearningNote,
    ): Flashcard {
        validateGeneratedLearningNoteUseCase(learningNote).requireValid()

        ensureUniqueFlashcardInDeckUseCase(deckId = deckId, note = learningNote)

        val input = generatedLearningNoteMapper.toCreateFlashcardInput(
            deckId = deckId,
            note = learningNote,
        )

        val flashcardId = repository.create(input)

        repository.upsertExamples(generatedLearningNoteMapper.toExamples(learningNote), flashcardId)

        return repository.fetchById(flashcardId).flashcard
    }
}

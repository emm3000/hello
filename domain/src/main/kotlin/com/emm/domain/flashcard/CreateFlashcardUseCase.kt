package com.emm.domain.flashcard

import com.emm.domain.ids.toDeckId
import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.requireValid

class CreateFlashcardUseCase(
    private val writeRepository: FlashcardWriteRepository,
    private val readRepository: FlashcardReadRepository,
    private val validateGeneratedLearningNoteUseCase: ValidateGeneratedLearningNoteUseCase,
    private val ensureUniqueFlashcardInDeckUseCase: EnsureUniqueFlashcardInDeckUseCase,
    private val generatedLearningNoteMapper: GeneratedLearningNoteMapper = GeneratedLearningNoteMapper(),
) {

    suspend operator fun invoke(
        deckId: String,
        learningNote: GeneratedLearningNote,
    ): Flashcard {
        val typedDeckId = deckId.toDeckId()

        validateGeneratedLearningNoteUseCase(learningNote).requireValid()

        ensureUniqueFlashcardInDeckUseCase(deckId = typedDeckId, note = learningNote)

        val input = generatedLearningNoteMapper.toCreateFlashcardInput(
            deckId = typedDeckId,
            note = learningNote,
        )

        val flashcardId: String = writeRepository.create(input)

        writeRepository.upsertExamples(generatedLearningNoteMapper.toExamples(learningNote), flashcardId)

        return readRepository.fetchById(flashcardId)
    }
}

package com.emm.domain.flashcard

import com.emm.domain.validation.DomainValidationException

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
        val validation = validateGeneratedLearningNoteUseCase(learningNote)
        if (!validation.isValid) {
            throw DomainValidationException(validation.errors)
        }

        ensureUniqueFlashcardInDeckUseCase(deckId = deckId, note = learningNote)

        val input = generatedLearningNoteMapper.toCreateFlashcardInput(
            deckId = deckId,
            note = learningNote,
        )

        val flashcardId: String = writeRepository.create(input)

        writeRepository.upsertExamples(generatedLearningNoteMapper.toExamples(learningNote), flashcardId)

        return readRepository.fetchById(flashcardId)
    }
}

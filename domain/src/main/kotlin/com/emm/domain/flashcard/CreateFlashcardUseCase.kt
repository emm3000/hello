package com.emm.domain.flashcard

import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue

class CreateFlashcardUseCase(
    private val writeRepository: FlashcardWriteRepository,
    private val readRepository: FlashcardReadRepository,
    private val validateGeneratedLearningNoteUseCase: ValidateGeneratedLearningNoteUseCase,
    private val isExactDuplicateGeneratedNoteUseCase: IsExactDuplicateGeneratedNoteUseCase,
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

        val isDuplicate = isExactDuplicateGeneratedNoteUseCase(deckId = deckId, note = learningNote)
        if (isDuplicate) {
            throw DomainValidationException(
                issues = listOf(
                    ValidationIssue.Error(
                        code = IssueCode.DuplicateExactCardInDeck,
                        field = "deckId",
                    )
                )
            )
        }

        val input = generatedLearningNoteMapper.toCreateFlashcardInput(
            deckId = deckId,
            note = learningNote,
        )

        val flashcardId: String = writeRepository.create(input)

        writeRepository.upsertExamples(generatedLearningNoteMapper.toExamples(learningNote), flashcardId)

        return readRepository.fetchById(flashcardId)
    }
}

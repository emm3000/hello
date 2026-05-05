package com.emm.domain.flashcard

import com.emm.domain.ids.DeckId
import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue

class EnsureUniqueFlashcardInDeckUseCase(
    private val isExactDuplicateGeneratedNoteUseCase: IsExactDuplicateGeneratedNoteUseCase,
) {

    suspend operator fun invoke(
        deckId: DeckId,
        note: GeneratedLearningNote,
    ) {
        if (!isExactDuplicateGeneratedNoteUseCase(deckId = deckId.value, note = note)) return
        throw duplicateExactCardException()
    }

    private fun duplicateExactCardException(): DomainValidationException {
        return DomainValidationException(issues = listOf(duplicateExactCardIssue()))
    }

    private fun duplicateExactCardIssue(): ValidationIssue.Error {
        return ValidationIssue.Error(
            code = IssueCode.DuplicateExactCardInDeck,
            field = "deckId",
        )
    }
}

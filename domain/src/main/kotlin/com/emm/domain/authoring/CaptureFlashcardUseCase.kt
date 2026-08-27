package com.emm.domain.authoring

import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.Expression
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue

class CaptureFlashcardUseCase(
    private val repository: FlashcardRepository,
    private val duplicateRepository: FlashcardDuplicateRepository,
) {

    suspend operator fun invoke(deckId: DeckId, word: String): FlashcardId {
        val expression: Expression = Expression.fromOrNull(word)
            ?: throw rejectedWordException(IssueCode.EmptyUserText)

        if (duplicateRepository.existsExpressionInDeck(deckId = deckId, expression = expression)) {
            throw rejectedWordException(IssueCode.DuplicateWordInDeck)
        }

        return repository.create(pendingFlashcardInput(deckId = deckId, expression = expression))
    }

    private fun pendingFlashcardInput(deckId: DeckId, expression: Expression): CreateFlashcardInput {
        return CreateFlashcardInput(
            deckId = deckId,
            word = expression.value,
            meaning = "",
            translation = "",
            phonetic = "",
            enrichmentStatus = EnrichmentStatus.PENDING,
        )
    }

    private fun rejectedWordException(code: IssueCode): DomainValidationException {
        return DomainValidationException(
            issues = listOf(ValidationIssue.Error(code = code, field = "word")),
        )
    }
}

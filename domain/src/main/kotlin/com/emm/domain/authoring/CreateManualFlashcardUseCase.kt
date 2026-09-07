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

class CreateManualFlashcardUseCase(
    private val repository: FlashcardRepository,
    private val duplicateRepository: FlashcardDuplicateRepository,
) {

    suspend operator fun invoke(
        deckId: DeckId,
        word: String,
        translation: String,
        meaning: String = "",
    ): FlashcardId {
        val expression: Expression = requireExpression(word)
        val writtenTranslation: String = requireTranslation(translation)

        if (duplicateRepository.existsExpressionInDeck(deckId = deckId, expression = expression)) {
            throw rejectedException(code = IssueCode.DuplicateWordInDeck, field = "word")
        }

        return repository.create(
            studiableFlashcardInput(
                deckId = deckId,
                expression = expression,
                translation = writtenTranslation,
                meaning = meaning.trim(),
            ),
        )
    }

    private fun requireExpression(word: String): Expression {
        return Expression.fromOrNull(word)
            ?: throw rejectedException(code = IssueCode.EmptyUserText, field = "word")
    }

    private fun requireTranslation(translation: String): String {
        val written: String = translation.trim()
        if (written.isEmpty()) {
            throw rejectedException(code = IssueCode.EmptyTranslation, field = "translation")
        }
        return written
    }

    private fun studiableFlashcardInput(
        deckId: DeckId,
        expression: Expression,
        translation: String,
        meaning: String,
    ): CreateFlashcardInput {
        return CreateFlashcardInput(
            deckId = deckId,
            word = expression.value,
            meaning = meaning,
            translation = translation,
            phonetic = "",
            enrichmentStatus = EnrichmentStatus.ENRICHED,
        )
    }

    private fun rejectedException(code: IssueCode, field: String): DomainValidationException {
        return DomainValidationException(
            issues = listOf(ValidationIssue.Error(code = code, field = field)),
        )
    }
}

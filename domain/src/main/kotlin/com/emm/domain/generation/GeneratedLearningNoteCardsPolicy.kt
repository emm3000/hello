package com.emm.domain.generation

import com.emm.domain.text.lowercaseRoot
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue

class GeneratedLearningNoteCardsPolicy {

    fun collectIssues(note: GeneratedLearningNote): CardsValidationResult {
        val errors = mutableListOf<ValidationIssue.Error>()
        val warnings = mutableListOf<ValidationIssue.Warning>()

        val activeCards = note.cards.filter { it.isActive }
        if (activeCards.isEmpty()) {
            errors += ValidationIssue.Error(
                code = IssueCode.NoActiveCards,
                field = "cards",
            )
        }

        if (activeCards.size > MAX_RECOMMENDED_ACTIVE_CARDS) {
            warnings += ValidationIssue.Warning(
                code = IssueCode.TooManyActiveCards,
                field = "cards",
            )
        }

        val duplicatedActiveCards: Map<String, GeneratedStudyCard> = activeCards
            .groupBy { "${it.prompt.normalizeForComparison()}::${it.expectedAnswer.normalizeForComparison()}" }
            .filterValues { it.size > 1 }
            .values
            .flatten()
            .associateBy { it.cardId }

        note.cards.forEach { card ->
            validateCard(card, duplicatedActiveCards, errors, warnings)
        }

        return CardsValidationResult(errors = errors, warnings = warnings)
    }

    private fun validateCard(
        card: GeneratedStudyCard,
        duplicatedActiveCards: Map<String, GeneratedStudyCard>,
        errors: MutableList<ValidationIssue.Error>,
        warnings: MutableList<ValidationIssue.Warning>,
    ) {
        if (card.prompt.isBlank()) {
            errors += ValidationIssue.Error(
                code = IssueCode.EmptyCardPrompt,
                field = "cards[${card.cardId}].prompt",
            )
        }
        if (card.expectedAnswer.isBlank()) {
            errors += ValidationIssue.Error(
                code = IssueCode.EmptyCardAnswer,
                field = "cards[${card.cardId}].expectedAnswer",
            )
        }
        if (
            card.prompt.isNotBlank() &&
            card.expectedAnswer.isNotBlank() &&
            card.prompt.normalizeForComparison() == card.expectedAnswer.normalizeForComparison()
        ) {
            errors += ValidationIssue.Error(
                code = IssueCode.CardPromptMatchesAnswer,
                field = "cards[${card.cardId}].expectedAnswer",
            )
        }
        if (card.isActive && duplicatedActiveCards.containsKey(card.cardId)) {
            warnings += ValidationIssue.Warning(
                code = IssueCode.DuplicateActiveCard,
                field = "cards[${card.cardId}].duplicate",
            )
        }
        if (card.expectedAnswer.wordCountForRecall() > MAX_RECOMMENDED_ANSWER_WORDS) {
            warnings += ValidationIssue.Warning(
                code = IssueCode.AnswerTooLongForRecall,
                field = "cards[${card.cardId}].expectedAnswer",
            )
        }
        if (!card.isActive) {
            warnings += ValidationIssue.Warning(
                code = IssueCode.InactiveCard,
                field = "cards[${card.cardId}].active",
            )
        }
    }

    private fun String.normalizeForComparison(): String {
        return trim().lowercaseRoot().replace("\\s+".toRegex(), " ")
    }

    private fun String.wordCountForRecall(): Int {
        if (isBlank()) return 0
        return trim().split("\\s+".toRegex()).size
    }

    private companion object {
        const val MAX_RECOMMENDED_ACTIVE_CARDS = 4
        const val MAX_RECOMMENDED_ANSWER_WORDS = 8
    }
}

data class CardsValidationResult(
    val errors: List<ValidationIssue.Error>,
    val warnings: List<ValidationIssue.Warning>,
)

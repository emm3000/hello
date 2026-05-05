package com.emm.domain.flashcard

import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue

class GeneratedLearningNoteCoreFieldsPolicy {

    fun collectIssues(note: GeneratedLearningNote): List<ValidationIssue.Error> {
        val errors = mutableListOf<ValidationIssue.Error>()

        requireNonBlank(
            value = note.noteId,
            code = IssueCode.MissingNoteId,
            errors = errors,
            field = "noteId",
        )
        requireNonBlank(
            value = note.expression,
            code = IssueCode.MissingExpression,
            errors = errors,
            field = "expression",
        )
        requireNonBlank(
            value = note.intendedMeaningEs,
            code = IssueCode.MissingIntendedMeaning,
            errors = errors,
            field = "intendedMeaningEs",
        )
        requireNonBlank(
            value = note.simpleDefinitionEn,
            code = IssueCode.MissingDefinition,
            errors = errors,
            field = "simpleDefinitionEn",
        )
        requireNonBlank(
            value = note.whyUseful,
            code = IssueCode.MissingWhyUseful,
            errors = errors,
            field = "whyUseful",
        )
        requireNonBlank(
            value = note.exampleSentence,
            code = IssueCode.MissingExampleSentence,
            errors = errors,
            field = "exampleSentence",
        )
        requireNonBlank(
            value = note.exampleTranslation,
            code = IssueCode.MissingExampleTranslation,
            errors = errors,
            field = "exampleTranslation",
        )

        if (note.cards.isEmpty()) {
            errors += ValidationIssue.Error(
                code = IssueCode.MissingCards,
                field = "cards",
            )
        }
        if (note.qualityChecks.isEmpty()) {
            errors += ValidationIssue.Error(
                code = IssueCode.MissingQualityChecks,
                field = "qualityChecks",
            )
        }

        return errors
    }

    private fun requireNonBlank(
        value: String,
        code: IssueCode,
        errors: MutableList<ValidationIssue.Error>,
        field: String,
    ) {
        if (value.isBlank()) {
            errors += ValidationIssue.Error(code = code, field = field)
        }
    }
}

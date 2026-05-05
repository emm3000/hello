package com.emm.domain.flashcard

import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import com.emm.domain.validation.ValidationResult

class ValidateGeneratedLearningNoteUseCase(
    private val typeRequirementsPolicy: GeneratedLearningNoteTypeRequirementsPolicy =
        GeneratedLearningNoteTypeRequirementsPolicy(),
    private val cardsPolicy: GeneratedLearningNoteCardsPolicy = GeneratedLearningNoteCardsPolicy(),
    private val qualityChecksPolicy: GeneratedLearningNoteQualityChecksPolicy =
        GeneratedLearningNoteQualityChecksPolicy(),
) {

    operator fun invoke(note: GeneratedLearningNote): ValidationResult<GeneratedLearningNote> {
        val errors = mutableListOf<ValidationIssue.Error>()
        val warnings = mutableListOf<ValidationIssue.Warning>()

        validateCoreFields(note, errors)
        errors += typeRequirementsPolicy.collectIssues(note)
        val cardsValidation = cardsPolicy.collectIssues(note)
        errors += cardsValidation.errors
        warnings += cardsValidation.warnings
        errors += qualityChecksPolicy.collectIssues(note)

        return if (errors.isEmpty()) {
            ValidationResult.valid(value = note, warnings = warnings)
        } else {
            ValidationResult.invalid(value = note, errors = errors, warnings = warnings)
        }
    }

    private fun validateCoreFields(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        validateIdentityFields(note, errors)
        validateMeaningFields(note, errors)
        validateExampleFields(note, errors)
        validateCollections(note, errors)
    }

    private fun validateIdentityFields(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        requireNonBlank(
            note.noteId,
            IssueCode.MissingNoteId,
            errors,
            field = "noteId",
        )
        requireNonBlank(
            note.expression,
            IssueCode.MissingExpression,
            errors,
            field = "expression",
        )
    }

    private fun validateMeaningFields(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        requireNonBlank(
            note.intendedMeaningEs,
            IssueCode.MissingIntendedMeaning,
            errors,
            field = "intendedMeaningEs",
        )
        requireNonBlank(
            note.simpleDefinitionEn,
            IssueCode.MissingDefinition,
            errors,
            field = "simpleDefinitionEn",
        )
        requireNonBlank(
            note.whyUseful,
            IssueCode.MissingWhyUseful,
            errors,
            field = "whyUseful",
        )
    }

    private fun validateExampleFields(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        requireNonBlank(
            note.exampleSentence,
            IssueCode.MissingExampleSentence,
            errors,
            field = "exampleSentence",
        )
        requireNonBlank(
            note.exampleTranslation,
            IssueCode.MissingExampleTranslation,
            errors,
            field = "exampleTranslation",
        )
    }

    private fun validateCollections(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
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

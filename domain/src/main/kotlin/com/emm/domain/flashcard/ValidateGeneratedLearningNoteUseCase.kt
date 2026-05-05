package com.emm.domain.flashcard

import com.emm.domain.text.lowercaseRoot
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import com.emm.domain.validation.ValidationResult

class ValidateGeneratedLearningNoteUseCase {

    private val qualityChecksPolicy = GeneratedLearningNoteQualityChecksPolicy()
    private val cardsPolicy = GeneratedLearningNoteCardsPolicy()

    operator fun invoke(note: GeneratedLearningNote): ValidationResult<GeneratedLearningNote> {
        val errors = mutableListOf<ValidationIssue.Error>()
        val warnings = mutableListOf<ValidationIssue.Warning>()

        validateCoreFields(note, errors)
        validateNoteTypeRequirements(note, errors)
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

    private fun validateNoteTypeRequirements(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        when (note.noteType) {
            LearningNoteType.Word -> validateWordNote(note, errors)
            LearningNoteType.Phrase -> validatePhraseNote(note, errors)
            LearningNoteType.PhrasalVerb -> validatePhrasalVerbNote(note, errors)
            LearningNoteType.Idiom -> validateIdiomNote(note, errors)
            LearningNoteType.SentencePattern -> validateSentencePatternNote(note, errors)
        }
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

    private fun validateWordNote(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        requireExpectedCard(note, StudyCardType.Recognition, errors)
        requireExpectedCard(note, StudyCardType.Production, errors)
    }

    private fun validatePhraseNote(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        requireUsagePattern(note, errors)
        requireExpectedCard(note, StudyCardType.Recognition, errors)
        requireExpectedCard(note, StudyCardType.Production, errors)
        requireExpectedCard(note, StudyCardType.Cloze, errors)
    }

    private fun validatePhrasalVerbNote(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        requireUsagePattern(note, errors)
        requireExpectedCard(note, StudyCardType.Recognition, errors)
        requireExpectedCard(note, StudyCardType.Production, errors)
        requireExpectedCard(note, StudyCardType.Cloze, errors)
    }

    private fun validateIdiomNote(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        requireExpectedCard(note, StudyCardType.Recognition, errors)
        requireExpectedCard(note, StudyCardType.Production, errors)
    }

    private fun validateSentencePatternNote(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        requireUsagePattern(note, errors)
        requireNonBlank(
            note.clozeSentence,
            IssueCode.MissingClozeSentence,
            errors,
            field = "clozeSentence",
        )
        requireExpectedCard(note, StudyCardType.Production, errors)
        requireExpectedCard(note, StudyCardType.Cloze, errors)
    }

    private fun requireUsagePattern(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        requireNonBlank(
            note.usagePattern,
            IssueCode.MissingUsagePattern,
            errors,
            field = "usagePattern",
        )
    }

    private fun requireExpectedCard(
        note: GeneratedLearningNote,
        cardType: StudyCardType,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        val hasCard = note.cards.any { it.cardType == cardType && it.isActive }
        if (!hasCard) {
            errors += ValidationIssue.Error(
                code = IssueCode.MissingExpectedCardType,
                field = "cards.expected.${cardType.name.lowercaseRoot()}",
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

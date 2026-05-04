package com.emm.domain.flashcard

import com.emm.domain.text.lowercaseRoot
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import com.emm.domain.validation.ValidationResult

class ValidateGeneratedLearningNoteUseCase {

    operator fun invoke(note: GeneratedLearningNote): ValidationResult<GeneratedLearningNote> {
        val errors = mutableListOf<ValidationIssue.Error>()
        val warnings = mutableListOf<ValidationIssue.Warning>()

        validateCoreFields(note, errors)
        validateNoteTypeRequirements(note, errors)
        validateCards(note, errors, warnings)
        validateQualityChecks(note, errors)

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

    private fun validateCards(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
        warnings: MutableList<ValidationIssue.Warning>,
    ) {
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

    private fun validateQualityChecks(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        val presentCodes = note.qualityChecks.map { it.code }.toSet()
        val hasSingleMeaningCheck = presentCodes.contains(GeneratedNoteQualityCode.SingleMeaning)
        if (!hasSingleMeaningCheck) {
            errors += ValidationIssue.Error(
                code = IssueCode.MissingSingleMeaningQualityCheck,
                field = "qualityChecks.singleMeaning",
            )
        }

        GeneratedNoteQualityCode.entries
            .filterNot(presentCodes::contains)
            .forEach { missingCode ->
                errors += ValidationIssue.Error(
                    code = IssueCode.MissingRequiredQualityCheck,
                    field = "qualityChecks.${missingCode.name.lowercaseRoot()}",
                )
            }

        val failedChecks = note.qualityChecks.filterNot { it.passed }
        if (failedChecks.isNotEmpty()) {
            errors += ValidationIssue.Error(
                code = IssueCode.FailedQualityCheck,
                field = "qualityChecks.failed",
            )
        }
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

    private companion object {
        const val MAX_RECOMMENDED_ACTIVE_CARDS = 4
        const val MAX_RECOMMENDED_ANSWER_WORDS = 8
    }
}

private fun String.normalizeForComparison(): String {
    return trim().lowercaseRoot().replace("\\s+".toRegex(), " ")
}

private fun String.wordCountForRecall(): Int {
    if (isBlank()) return 0
    return trim().split("\\s+".toRegex()).size
}

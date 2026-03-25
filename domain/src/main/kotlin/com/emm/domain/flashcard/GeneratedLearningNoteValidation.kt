package com.emm.domain.flashcard

data class GeneratedLearningNoteValidation(
    val errors: List<GeneratedLearningNoteIssue>,
    val warnings: List<GeneratedLearningNoteIssue>,
) {
    val isValid: Boolean
        get() = errors.isEmpty()
}

data class GeneratedLearningNoteIssue(
    val code: GeneratedLearningNoteIssueCode,
    val message: String,
    val noteField: String? = null,
    val cardId: String? = null,
)

enum class GeneratedLearningNoteIssueCode {
    MissingNoteId,
    MissingExpression,
    MissingIntendedMeaning,
    MissingDefinition,
    MissingWhyUseful,
    MissingExampleSentence,
    MissingExampleTranslation,
    MissingCards,
    MissingQualityChecks,
    MissingUsagePattern,
    MissingClozeSentence,
    MissingExpectedCardType,
    EmptyCardPrompt,
    EmptyCardAnswer,
    InactiveCard,
    MissingSingleMeaningQualityCheck,
    NoActiveCards,
    FailedQualityCheck,
}

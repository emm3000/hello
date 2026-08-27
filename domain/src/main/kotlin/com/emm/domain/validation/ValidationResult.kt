package com.emm.domain.validation

sealed interface IssueCode {
    val value: String

    data object EmptyUserText : IssueCode { override val value: String = "empty_user_text" }
    data object WordInputContainsWhitespace : IssueCode {
        override val value: String = "word_input_contains_whitespace"
    }
    data object PhraseInputTooShort : IssueCode { override val value: String = "phrase_input_too_short" }
    data object SentenceInputTooShort : IssueCode { override val value: String = "sentence_input_too_short" }
    data object MissingDisambiguation : IssueCode { override val value: String = "missing_disambiguation" }
    data object MissingCommunicativeIntent : IssueCode { override val value: String = "missing_communicative_intent" }
    data object CommunicativeGoalTooShort : IssueCode { override val value: String = "communicative_goal_too_short" }
    data object ContextSentenceTooShort : IssueCode { override val value: String = "context_sentence_too_short" }
    data object MissingNoteId : IssueCode { override val value: String = "missing_note_id" }
    data object MissingExpression : IssueCode { override val value: String = "missing_expression" }
    data object MissingIntendedMeaning : IssueCode { override val value: String = "missing_intended_meaning" }
    data object MissingDefinition : IssueCode { override val value: String = "missing_definition" }
    data object MissingWhyUseful : IssueCode { override val value: String = "missing_why_useful" }
    data object MissingExampleSentence : IssueCode { override val value: String = "missing_example_sentence" }
    data object MissingExampleTranslation : IssueCode { override val value: String = "missing_example_translation" }
    data object MissingCards : IssueCode { override val value: String = "missing_cards" }
    data object MissingQualityChecks : IssueCode { override val value: String = "missing_quality_checks" }
    data object MissingUsagePattern : IssueCode { override val value: String = "missing_usage_pattern" }
    data object MissingClozeSentence : IssueCode { override val value: String = "missing_cloze_sentence" }
    data object MissingExpectedCardType : IssueCode { override val value: String = "missing_expected_card_type" }
    data object EmptyCardPrompt : IssueCode { override val value: String = "empty_card_prompt" }
    data object EmptyCardAnswer : IssueCode { override val value: String = "empty_card_answer" }
    data object InactiveCard : IssueCode { override val value: String = "inactive_card" }
    data object MissingSingleMeaningQualityCheck : IssueCode {
        override val value: String = "missing_single_meaning_quality_check"
    }
    data object MissingRequiredQualityCheck : IssueCode {
        override val value: String = "missing_required_quality_check"
    }
    data object NoActiveCards : IssueCode { override val value: String = "no_active_cards" }
    data object FailedQualityCheck : IssueCode { override val value: String = "failed_quality_check" }
    data object CardPromptMatchesAnswer : IssueCode { override val value: String = "card_prompt_matches_answer" }
    data object DuplicateActiveCard : IssueCode { override val value: String = "duplicate_active_card" }
    data object DuplicateExactCardInDeck : IssueCode { override val value: String = "duplicate_exact_card_in_deck" }
    data object DuplicateWordInDeck : IssueCode { override val value: String = "duplicate_word_in_deck" }
    data object TooManyActiveCards : IssueCode { override val value: String = "too_many_active_cards" }
    data object AnswerTooLongForRecall : IssueCode { override val value: String = "answer_too_long_for_recall" }

    companion object {
        val all: Set<IssueCode> = setOf(
            EmptyUserText,
            WordInputContainsWhitespace,
            PhraseInputTooShort,
            SentenceInputTooShort,
            MissingDisambiguation,
            MissingCommunicativeIntent,
            CommunicativeGoalTooShort,
            ContextSentenceTooShort,
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
            MissingRequiredQualityCheck,
            NoActiveCards,
            FailedQualityCheck,
            CardPromptMatchesAnswer,
            DuplicateActiveCard,
            DuplicateExactCardInDeck,
            DuplicateWordInDeck,
            TooManyActiveCards,
            AnswerTooLongForRecall,
        )
    }
}

enum class IssueSeverity {
    Error,
    Warning,
}

sealed interface ValidationIssue {
    val code: IssueCode
    val field: String
    val severity: IssueSeverity

    data class Error(
        override val code: IssueCode,
        override val field: String,
    ) : ValidationIssue {
        init {
            require(field.isNotBlank()) { "ValidationIssue field cannot be blank." }
        }

        override val severity: IssueSeverity = IssueSeverity.Error
    }

    data class Warning(
        override val code: IssueCode,
        override val field: String,
    ) : ValidationIssue {
        init {
            require(field.isNotBlank()) { "ValidationIssue field cannot be blank." }
        }

        override val severity: IssueSeverity = IssueSeverity.Warning
    }
}

sealed class ValidationResult<out T>(
    open val value: T,
    open val issues: List<ValidationIssue>,
) {
    val errors: List<ValidationIssue.Error>
        get() = issues.filterIsInstance<ValidationIssue.Error>()

    val warnings: List<ValidationIssue.Warning>
        get() = issues.filterIsInstance<ValidationIssue.Warning>()

    val isValid: Boolean
        get() = errors.isEmpty()

    data class Valid<T>(
        override val value: T,
        override val issues: List<ValidationIssue> = emptyList(),
    ) : ValidationResult<T>(value = value, issues = issues)

    data class Invalid<T>(
        override val value: T,
        override val issues: List<ValidationIssue>,
    ) : ValidationResult<T>(value = value, issues = issues) {
        init {
            require(issues.isNotEmpty()) { "Invalid validation result must contain issues." }
            require(issues.any { it.severity == IssueSeverity.Error }) {
                "Invalid validation result must contain at least one error."
            }
        }
    }

    companion object {
        fun <T> valid(
            value: T,
            warnings: List<ValidationIssue.Warning> = emptyList(),
        ): ValidationResult<T> = Valid(value = value, issues = warnings)

        fun <T> invalid(
            value: T,
            errors: List<ValidationIssue.Error>,
            warnings: List<ValidationIssue.Warning> = emptyList(),
        ): ValidationResult<T> = Invalid(value = value, issues = errors + warnings)
    }
}

package com.emm.hello.newfeatures.card.validation

import androidx.annotation.StringRes
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import com.emm.hello.R

class IssueTextMapper {

    fun map(issue: ValidationIssue): IssueTextMapping {
        val target = target(issue.field)
        return IssueTextMapping(
            textResId = textResId(issue.code, issue.field),
            target = target,
        )
    }

    @Suppress("CyclomaticComplexMethod")
    fun target(field: String): IssueUiTarget {
        return when {
            field == "userText" -> IssueUiTarget.Input(InputField.PrimaryText)
            field == "communicativeIntentId" -> IssueUiTarget.Input(InputField.Category)
            field == "contextSentence" -> IssueUiTarget.Input(InputField.ContextSentence)
            field == "disambiguation" -> IssueUiTarget.Input(InputField.Disambiguation)
            field == "intendedMeaningEs" -> IssueUiTarget.PreviewFieldTarget(PreviewField.IntendedMeaning)
            field == "simpleDefinitionEn" -> IssueUiTarget.PreviewFieldTarget(PreviewField.SimpleDefinition)
            field == "whyUseful" -> IssueUiTarget.PreviewFieldTarget(PreviewField.WhyUseful)
            field == "usagePattern" -> IssueUiTarget.PreviewFieldTarget(PreviewField.UsagePattern)
            field == "clozeSentence" -> IssueUiTarget.PreviewFieldTarget(PreviewField.ClozeSentence)
            field == "exampleSentence" -> IssueUiTarget.PreviewFieldTarget(PreviewField.ExampleSentence)
            field == "exampleTranslation" -> IssueUiTarget.PreviewFieldTarget(PreviewField.ExampleTranslation)
            field.startsWith("cards[") -> field.toCardTarget()
            field == "cards" ||
                field.startsWith("cards.expected.") ||
                field == "noteId" ||
                field == "expression" ||
                field == "qualityChecks" ||
                field.startsWith("qualityChecks.") -> IssueUiTarget.PreviewSummary
            else -> IssueUiTarget.Global
        }
    }

    @Suppress("CyclomaticComplexMethod")
    @StringRes
    private fun textResId(
        code: IssueCode,
        field: String,
    ): Int {
        return when (code) {
            IssueCode.EmptyUserText -> R.string.validation_empty_user_text
            IssueCode.WordInputContainsWhitespace -> R.string.validation_word_input_contains_whitespace
            IssueCode.PhraseInputTooShort -> R.string.validation_phrase_input_too_short
            IssueCode.SentenceInputTooShort -> R.string.validation_sentence_input_too_short
            IssueCode.MissingDisambiguation -> R.string.validation_missing_disambiguation
            IssueCode.MissingCommunicativeIntent -> R.string.validation_missing_communicative_intent
            IssueCode.CommunicativeGoalTooShort -> R.string.validation_communicative_goal_too_short
            IssueCode.ContextSentenceTooShort -> R.string.validation_context_sentence_too_short
            IssueCode.MissingNoteId -> R.string.validation_missing_note_id
            IssueCode.MissingExpression -> R.string.validation_missing_expression
            IssueCode.MissingIntendedMeaning -> R.string.validation_missing_intended_meaning
            IssueCode.MissingDefinition -> R.string.validation_missing_definition
            IssueCode.MissingWhyUseful -> R.string.validation_missing_why_useful
            IssueCode.MissingExampleSentence -> R.string.validation_missing_example_sentence
            IssueCode.MissingExampleTranslation -> R.string.validation_missing_example_translation
            IssueCode.MissingCards -> R.string.validation_missing_cards
            IssueCode.MissingQualityChecks -> R.string.validation_missing_quality_checks
            IssueCode.MissingUsagePattern -> R.string.validation_missing_usage_pattern
            IssueCode.MissingClozeSentence -> R.string.validation_missing_cloze_sentence
            IssueCode.MissingExpectedCardType -> field.expectedCardTextResId()
            IssueCode.EmptyCardPrompt -> R.string.validation_empty_card_prompt
            IssueCode.EmptyCardAnswer -> R.string.validation_empty_card_answer
            IssueCode.InactiveCard -> R.string.validation_inactive_card
            IssueCode.MissingSingleMeaningQualityCheck -> R.string.validation_missing_single_meaning_quality_check
            IssueCode.MissingRequiredQualityCheck -> R.string.validation_missing_required_quality_check
            IssueCode.NoActiveCards -> R.string.validation_no_active_cards
            IssueCode.FailedQualityCheck -> R.string.validation_failed_quality_check
            IssueCode.CardPromptMatchesAnswer -> R.string.validation_card_prompt_matches_answer
            IssueCode.DuplicateActiveCard -> R.string.validation_duplicate_active_card
            IssueCode.TooManyActiveCards -> R.string.validation_too_many_active_cards
            IssueCode.AnswerTooLongForRecall -> R.string.validation_answer_too_long_for_recall
        }
    }
}

data class IssueTextMapping(
    @StringRes val textResId: Int,
    val target: IssueUiTarget,
)

sealed interface IssueUiTarget {
    data class Input(val field: InputField) : IssueUiTarget
    data class PreviewFieldTarget(val field: PreviewField) : IssueUiTarget
    data class PreviewCard(val cardId: String, val field: PreviewCardField) : IssueUiTarget
    data object PreviewSummary : IssueUiTarget
    data object Global : IssueUiTarget
}

enum class InputField {
    PrimaryText,
    Disambiguation,
    ContextSentence,
    Category,
}

enum class PreviewField {
    IntendedMeaning,
    SimpleDefinition,
    WhyUseful,
    UsagePattern,
    ClozeSentence,
    ExampleSentence,
    ExampleTranslation,
}

enum class PreviewCardField {
    Prompt,
    ExpectedAnswer,
    Active,
    Summary,
}

private val cardFieldRegex = Regex("""cards\[(.+)]\.(.+)""")

private fun String.toCardTarget(): IssueUiTarget {
    val match = cardFieldRegex.matchEntire(this) ?: return IssueUiTarget.PreviewSummary
    val cardId = match.groupValues[1]
    val rawField = match.groupValues[2]
    val field = when (rawField) {
        "prompt" -> PreviewCardField.Prompt
        "expectedAnswer" -> PreviewCardField.ExpectedAnswer
        "active" -> PreviewCardField.Active
        else -> PreviewCardField.Summary
    }
    return IssueUiTarget.PreviewCard(cardId = cardId, field = field)
}

@StringRes
private fun String.expectedCardTextResId(): Int {
    return when {
        endsWith("recognition") -> R.string.validation_missing_expected_recognition_card
        endsWith("production") -> R.string.validation_missing_expected_production_card
        endsWith("cloze") -> R.string.validation_missing_expected_cloze_card
        endsWith("form") -> R.string.validation_missing_expected_form_card
        else -> R.string.validation_missing_expected_card_type
    }
}

package com.emm.domain.flashcard

import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue

class FlashcardGenerationInputTypeRulesPolicy {

    fun validate(
        input: FlashcardGenerationInput,
        userText: String,
        wordCount: Int,
    ): InputTypeRuleResult {
        val errors = mutableListOf<ValidationIssue.Error>()
        val warnings = mutableListOf<ValidationIssue.Warning>()

        when (input.inputType) {
            FlashcardInputType.Word -> {
                if (wordCount > 1) {
                    warnings += ValidationIssue.Warning(
                        code = IssueCode.WordInputContainsWhitespace,
                        field = "userText",
                    )
                }
            }

            FlashcardInputType.Phrase -> {
                if (userText.isNotBlank() && wordCount < MIN_PHRASE_WORD_COUNT) {
                    warnings += ValidationIssue.Warning(
                        code = IssueCode.PhraseInputTooShort,
                        field = "userText",
                    )
                }
            }

            FlashcardInputType.Sentence -> {
                if (userText.isNotBlank() && wordCount < MIN_SENTENCE_WORD_COUNT) {
                    errors += ValidationIssue.Error(
                        code = IssueCode.SentenceInputTooShort,
                        field = "userText",
                    )
                }
            }

            FlashcardInputType.CommunicativeGoal -> {
                if (input.communicativeIntentId.isBlank()) {
                    errors += ValidationIssue.Error(
                        code = IssueCode.MissingCommunicativeIntent,
                        field = "communicativeIntentId",
                    )
                }
                if (userText.isNotBlank() && userText.length < MIN_COMMUNICATIVE_GOAL_LENGTH) {
                    warnings += ValidationIssue.Warning(
                        code = IssueCode.CommunicativeGoalTooShort,
                        field = "userText",
                    )
                }
            }
        }

        return InputTypeRuleResult(errors = errors, warnings = warnings)
    }

    private companion object {
        const val MIN_PHRASE_WORD_COUNT = 2
        const val MIN_SENTENCE_WORD_COUNT = 4
        const val MIN_COMMUNICATIVE_GOAL_LENGTH = 8
    }
}

data class InputTypeRuleResult(
    val errors: List<ValidationIssue.Error>,
    val warnings: List<ValidationIssue.Warning>,
)

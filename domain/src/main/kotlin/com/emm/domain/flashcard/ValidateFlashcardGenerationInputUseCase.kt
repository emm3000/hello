package com.emm.domain.flashcard

import com.emm.domain.text.lowercaseRoot
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import com.emm.domain.validation.ValidationResult

class ValidateFlashcardGenerationInputUseCase {

    operator fun invoke(input: FlashcardGenerationInput): ValidationResult<FlashcardGenerationInput> {
        val normalized = input.normalized()
        val errors = mutableListOf<ValidationIssue.Error>()
        val warnings = mutableListOf<ValidationIssue.Warning>()
        val userText = normalized.userText
        val wordCount = wordCount(userText)

        validateRequiredUserText(userText, errors)
        validateInputTypeRules(normalized, userText, wordCount, errors, warnings)
        validateDisambiguation(normalized, wordCount, errors)
        validateContextSentence(normalized.contextSentence, warnings)

        return if (errors.isEmpty()) {
            ValidationResult.valid(value = normalized, warnings = warnings)
        } else {
            ValidationResult.invalid(value = normalized, errors = errors, warnings = warnings)
        }
    }

    private fun validateRequiredUserText(
        userText: String,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        if (userText.isBlank()) {
            errors += ValidationIssue.Error(
                code = IssueCode.EmptyUserText,
                field = "userText",
            )
        }
    }

    private fun validateInputTypeRules(
        input: FlashcardGenerationInput,
        userText: String,
        wordCount: Int,
        errors: MutableList<ValidationIssue.Error>,
        warnings: MutableList<ValidationIssue.Warning>,
    ) {
        when (input.inputType) {
            FlashcardInputType.Word -> validateWordInput(wordCount, warnings)
            FlashcardInputType.Phrase -> validatePhraseInput(userText, wordCount, warnings)
            FlashcardInputType.Sentence -> validateSentenceInput(userText, wordCount, errors)
            FlashcardInputType.CommunicativeGoal -> {
                validateCommunicativeGoalInput(input, userText, errors, warnings)
            }
        }
    }

    private fun validateWordInput(
        wordCount: Int,
        warnings: MutableList<ValidationIssue.Warning>,
    ) {
        if (wordCount > 1) {
            warnings += ValidationIssue.Warning(
                code = IssueCode.WordInputContainsWhitespace,
                field = "userText",
            )
        }
    }

    private fun validatePhraseInput(
        userText: String,
        wordCount: Int,
        warnings: MutableList<ValidationIssue.Warning>,
    ) {
        if (userText.isNotBlank() && wordCount < 2) {
            warnings += ValidationIssue.Warning(
                code = IssueCode.PhraseInputTooShort,
                field = "userText",
            )
        }
    }

    private fun validateSentenceInput(
        userText: String,
        wordCount: Int,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        if (userText.isNotBlank() && wordCount < MIN_SENTENCE_WORD_COUNT) {
            errors += ValidationIssue.Error(
                code = IssueCode.SentenceInputTooShort,
                field = "userText",
            )
        }
    }

    private fun validateCommunicativeGoalInput(
        input: FlashcardGenerationInput,
        userText: String,
        errors: MutableList<ValidationIssue.Error>,
        warnings: MutableList<ValidationIssue.Warning>,
    ) {
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

    private fun validateDisambiguation(
        input: FlashcardGenerationInput,
        wordCount: Int,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        val needsDisambiguation = requiresDisambiguation(
            inputType = input.inputType,
            normalizedUserText = input.userText.lowercaseRoot(),
            wordCount = wordCount,
        )
        val hasDisambiguation = input.intendedMeaningEs.isNotBlank() || input.contextSentence.isNotBlank()
        if (needsDisambiguation && !hasDisambiguation) {
            errors += ValidationIssue.Error(
                code = IssueCode.MissingDisambiguation,
                field = "disambiguation",
            )
        }
    }

    private fun validateContextSentence(
        contextSentence: String,
        warnings: MutableList<ValidationIssue.Warning>,
    ) {
        if (contextSentence.isNotBlank() && wordCount(contextSentence) < MIN_CONTEXT_WORD_COUNT) {
            warnings += ValidationIssue.Warning(
                code = IssueCode.ContextSentenceTooShort,
                field = "contextSentence",
            )
        }
    }

    private fun requiresDisambiguation(
        inputType: FlashcardInputType,
        normalizedUserText: String,
        wordCount: Int,
    ): Boolean {
        val supportsDisambiguation = inputType != FlashcardInputType.Sentence &&
            inputType != FlashcardInputType.CommunicativeGoal
        val isKnownAmbiguousTarget = normalizedUserText in ambiguousTargets
        val isShortAmbiguousWord = inputType == FlashcardInputType.Word &&
            wordCount == 1 &&
            normalizedUserText.length <= SHORT_AMBIGUOUS_WORD_LENGTH

        return normalizedUserText.isNotBlank() &&
            supportsDisambiguation &&
            (isKnownAmbiguousTarget || isShortAmbiguousWord)
    }

    private fun wordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(WHITESPACE_REGEX).size
    }

    private companion object {
        val WHITESPACE_REGEX = "\\s+".toRegex()

        const val MIN_SENTENCE_WORD_COUNT = 4
        const val MIN_CONTEXT_WORD_COUNT = 4
        const val MIN_COMMUNICATIVE_GOAL_LENGTH = 8
        const val SHORT_AMBIGUOUS_WORD_LENGTH = 3

        val ambiguousTargets = setOf(
            "get",
            "set",
            "run",
            "take",
            "make",
            "do",
            "go",
            "come",
            "put",
            "have",
            "pick up",
            "take off",
            "look up",
            "run out",
            "figure out",
            "work out",
        )
    }
}

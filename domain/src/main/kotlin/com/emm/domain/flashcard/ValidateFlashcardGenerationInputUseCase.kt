package com.emm.domain.flashcard

import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import com.emm.domain.validation.ValidationResult

class ValidateFlashcardGenerationInputUseCase(
    private val disambiguationPolicy: FlashcardGenerationDisambiguationPolicy = FlashcardGenerationDisambiguationPolicy(),
    private val inputTypeRulesPolicy: FlashcardGenerationInputTypeRulesPolicy =
        FlashcardGenerationInputTypeRulesPolicy(),
) {

    operator fun invoke(input: FlashcardGenerationInput): ValidationResult<FlashcardGenerationInput> {
        val normalized = input.normalized()
        val errors = mutableListOf<ValidationIssue.Error>()
        val warnings = mutableListOf<ValidationIssue.Warning>()
        val userText = normalized.userText
        val wordCount = wordCount(userText)

        validateRequiredUserText(userText, errors)
        val inputTypeResult = inputTypeRulesPolicy.validate(
            input = normalized,
            userText = userText,
            wordCount = wordCount,
        )
        errors += inputTypeResult.errors
        warnings += inputTypeResult.warnings
        disambiguationPolicy
            .missingDisambiguationIssueOrNull(input = normalized, wordCount = wordCount)
            ?.let(errors::add)
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

    private fun wordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(WHITESPACE_REGEX).size
    }

    private companion object {
        val WHITESPACE_REGEX = "\\s+".toRegex()

        const val MIN_CONTEXT_WORD_COUNT = 4
    }
}

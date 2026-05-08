package com.emm.domain.flashcard

import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue
import com.emm.domain.validation.ValidationResult

class ValidateFlashcardGenerationInputUseCase(
    private val disambiguationPolicy: FlashcardGenerationDisambiguationPolicy =
        FlashcardGenerationDisambiguationPolicy(),
    private val inputTypeRulesPolicy: FlashcardGenerationInputTypeRulesPolicy =
        FlashcardGenerationInputTypeRulesPolicy(),
    private val contextSentencePolicy: FlashcardGenerationContextSentencePolicy =
        FlashcardGenerationContextSentencePolicy(),
) {

    operator fun invoke(input: FlashcardGenerationInput): ValidationResult<FlashcardGenerationInput> {
        val normalized = input.normalized()
        val errors = mutableListOf<ValidationIssue.Error>()
        val warnings = mutableListOf<ValidationIssue.Warning>()
        val userText = normalized.userText
        val wordCount = userText.wordCountNormalized()

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
        val contextWordCount = normalized.contextSentence.wordCountNormalized()
        contextSentencePolicy
            .contextSentenceWarningOrNull(
                contextSentence = normalized.contextSentence,
                wordCount = contextWordCount,
            )
            ?.let(warnings::add)

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
}

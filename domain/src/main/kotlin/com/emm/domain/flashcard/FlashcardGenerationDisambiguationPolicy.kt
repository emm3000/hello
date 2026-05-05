package com.emm.domain.flashcard

import com.emm.domain.text.lowercaseRoot
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue

class FlashcardGenerationDisambiguationPolicy {

    fun missingDisambiguationIssueOrNull(
        input: FlashcardGenerationInput,
        wordCount: Int,
    ): ValidationIssue.Error? {
        val needsDisambiguation = requiresDisambiguation(
            inputType = input.inputType,
            normalizedUserText = input.userText.lowercaseRoot(),
            wordCount = wordCount,
        )
        val hasDisambiguation = input.intendedMeaningEs.isNotBlank() || input.contextSentence.isNotBlank()
        if (needsDisambiguation && !hasDisambiguation) {
            return ValidationIssue.Error(
                code = IssueCode.MissingDisambiguation,
                field = "disambiguation",
            )
        }

        return null
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

    private companion object {
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

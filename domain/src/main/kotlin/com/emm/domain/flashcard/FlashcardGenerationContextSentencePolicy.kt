package com.emm.domain.flashcard

import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue

class FlashcardGenerationContextSentencePolicy {

    fun contextSentenceWarningOrNull(contextSentence: String, wordCount: Int): ValidationIssue.Warning? {
        if (contextSentence.isNotBlank() && wordCount < MIN_CONTEXT_WORD_COUNT) {
            return ValidationIssue.Warning(
                code = IssueCode.ContextSentenceTooShort,
                field = "contextSentence",
            )
        }

        return null
    }

    private companion object {
        const val MIN_CONTEXT_WORD_COUNT = 4
    }
}

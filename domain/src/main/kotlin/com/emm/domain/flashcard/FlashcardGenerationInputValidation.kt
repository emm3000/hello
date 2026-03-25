package com.emm.domain.flashcard

data class FlashcardGenerationInputValidation(
    val normalizedInput: FlashcardGenerationInput,
    val errors: List<FlashcardGenerationInputIssue>,
    val warnings: List<FlashcardGenerationInputIssue>,
) {
    val isValid: Boolean
        get() = errors.isEmpty()
}

data class FlashcardGenerationInputIssue(
    val code: FlashcardGenerationInputIssueCode,
    val message: String,
)

enum class FlashcardGenerationInputIssueCode {
    EmptyUserText,
    WordInputContainsWhitespace,
    PhraseInputTooShort,
    SentenceInputTooShort,
    MissingDisambiguation,
    MissingCommunicativeIntent,
    CommunicativeGoalTooShort,
    ContextSentenceTooShort,
}

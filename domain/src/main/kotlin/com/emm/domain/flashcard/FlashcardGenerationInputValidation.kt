package com.emm.domain.flashcard

import com.emm.domain.validation.ValidationIssue
import com.emm.domain.validation.ValidationResult

@Deprecated(
    message = "Use ValidationResult<FlashcardGenerationInput> directly.",
    replaceWith = ReplaceWith("ValidationResult<FlashcardGenerationInput>"),
)
typealias FlashcardGenerationInputValidation = ValidationResult<FlashcardGenerationInput>

@Deprecated(
    message = "Use ValidationIssue directly.",
    replaceWith = ReplaceWith("ValidationIssue"),
)
typealias FlashcardGenerationInputIssue = ValidationIssue

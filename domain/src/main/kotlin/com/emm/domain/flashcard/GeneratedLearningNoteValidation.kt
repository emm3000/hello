package com.emm.domain.flashcard

import com.emm.domain.validation.ValidationIssue
import com.emm.domain.validation.ValidationResult

@Deprecated(
    message = "Use ValidationResult<GeneratedLearningNote> directly.",
    replaceWith = ReplaceWith("ValidationResult<GeneratedLearningNote>"),
)
typealias GeneratedLearningNoteValidation = ValidationResult<GeneratedLearningNote>

@Deprecated(
    message = "Use ValidationIssue directly.",
    replaceWith = ReplaceWith("ValidationIssue"),
)
typealias GeneratedLearningNoteIssue = ValidationIssue

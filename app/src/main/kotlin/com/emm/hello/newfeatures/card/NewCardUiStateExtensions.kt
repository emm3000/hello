package com.emm.hello.newfeatures.card

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.validation.ValidationResult

internal fun NewCardUiState.clearPreviewState(
    error: NewCardErrorUi? = null,
    isLoading: Boolean = this.isLoading,
): NewCardUiState {
    return copy(
        error = error,
        inputValidationIssues = emptyList(),
        inputWarningIssues = emptyList(),
        learningNotePreview = null,
        previewGeneratedWarnings = emptyList(),
        previewValidationIssues = emptyList(),
        previewWarningIssues = emptyList(),
        canSavePreview = false,
        previewRegenerationTarget = null,
        isLoading = isLoading,
    )
}

internal fun NewCardUiState.withPreviewValidation(
    preview: GeneratedLearningNote,
    validation: ValidationResult<GeneratedLearningNote>,
    error: NewCardErrorUi? = null,
    isLoading: Boolean = false,
): NewCardUiState {
    return copy(
        learningNotePreview = preview,
        error = error,
        inputValidationIssues = emptyList(),
        inputWarningIssues = emptyList(),
        previewGeneratedWarnings = preview.warnings,
        previewValidationIssues = validation.errors,
        previewWarningIssues = validation.warnings,
        canSavePreview = validation.isValid,
        previewRegenerationTarget = null,
        isLoading = isLoading,
    )
}

internal fun NewCardUiState.resetAfterSave(): NewCardUiState {
    return clearPreviewState(isLoading = false).copy(
        word = "",
        aiRequest = "",
        intendedMeaningEs = "",
        contextSentence = "",
    )
}

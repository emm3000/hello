package com.emm.hello.newfeatures.card

import com.emm.domain.flashcard.GeneratedLearningNote
import com.emm.domain.flashcard.GeneratedLearningNoteIssue
import com.emm.domain.flashcard.GeneratedLearningNoteValidation

internal fun NewCardUiState.clearPreviewState(
    error: NewCardErrorUi? = null,
    isLoading: Boolean = this.isLoading,
): NewCardUiState {
    return copy(
        error = error,
        learningNotePreview = null,
        previewValidationErrors = emptyList(),
        previewWarnings = emptyList(),
        previewValidationIssues = emptyList(),
        previewWarningIssues = emptyList(),
        canSavePreview = false,
        previewRegenerationTarget = null,
        isLoading = isLoading,
    )
}

internal fun NewCardUiState.withPreviewValidation(
    preview: GeneratedLearningNote,
    validation: GeneratedLearningNoteValidation,
    error: NewCardErrorUi? = null,
    isLoading: Boolean = false,
): NewCardUiState {
    return copy(
        learningNotePreview = preview,
        error = error,
        previewValidationErrors = validation.errors.messages(),
        previewWarnings = validation.warnings.messages() + preview.warnings,
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

private fun List<GeneratedLearningNoteIssue>.messages(): List<String> {
    return map(GeneratedLearningNoteIssue::message)
}

package com.emm.hello.newfeatures.card

import com.emm.domain.flashcard.GeneratedLearningNote
import com.emm.domain.validation.DomainValidationException

class NewCardPreviewWorkflow(
    private val generationDependencies: NewCardGenerationDependencies,
) {

    suspend fun generate(state: NewCardUiState): NewCardPreviewResult {
        val inputValidation = generationDependencies.validateInputUseCase(state.toGenerationInput())
        if (!inputValidation.isValid) {
            return NewCardPreviewResult.InputInvalid(
                errors = inputValidation.errors,
                warnings = inputValidation.warnings,
            )
        }

        return runCatching {
            generationDependencies.generateLearningNotePreviewUseCase(input = inputValidation.value)
        }.fold(
            onSuccess = { preview ->
                val previewValidation = generationDependencies.validateGeneratedLearningNoteUseCase(preview)
                NewCardPreviewResult.PreviewReady(preview = preview, validation = previewValidation)
            },
            onFailure = { error ->
                if (error is DomainValidationException) {
                    NewCardPreviewResult.DomainError(error.issues)
                } else {
                    NewCardPreviewResult.UnexpectedError(error)
                }
            },
        )
    }

    suspend fun regenerateExample(state: NewCardUiState): NewCardPreviewUpdateResult {
        return runPreviewUpdate(state) { current, preview ->
            val example = generationDependencies.regenerateLearningNoteExampleUseCase(
                input = current.toGenerationInput(),
                note = preview,
            )
            preview.copy(
                exampleSentence = example.sentence,
                exampleTranslation = example.translation,
            )
        }
    }

    suspend fun regenerateCloze(state: NewCardUiState): NewCardPreviewUpdateResult {
        return runPreviewUpdate(state) { current, preview ->
            val cloze = generationDependencies.regenerateLearningNoteClozeUseCase(
                input = current.toGenerationInput(),
                note = preview,
            )
            preview.copy(clozeSentence = cloze)
        }
    }

    suspend fun regenerateField(
        state: NewCardUiState,
        field: EditableLearningNoteField,
    ): NewCardPreviewUpdateResult {
        val regenerableField = field.toRegenerableFieldOrNull() ?: return NewCardPreviewUpdateResult.NoPreview

        return runPreviewUpdate(state) { current, preview ->
            val value = generationDependencies.regenerateLearningNoteFieldUseCase(
                input = current.toGenerationInput(),
                note = preview,
                field = regenerableField,
            )
            when (field) {
                EditableLearningNoteField.WhyUseful -> preview.copy(whyUseful = value)
                EditableLearningNoteField.UsagePattern -> preview.copy(usagePattern = value)
                EditableLearningNoteField.CommonMistake -> preview.copy(commonMistake = value)
                else -> preview
            }
        }
    }

    suspend fun regenerateCard(
        state: NewCardUiState,
        cardId: String,
    ): NewCardPreviewUpdateResult {
        return runPreviewUpdate(state) { current, preview ->
            val regeneratedCard = generationDependencies.regenerateStudyCardUseCase(
                input = current.toGenerationInput(),
                note = preview,
                cardId = cardId,
            )
            preview.copy(
                cards = preview.cards.map { card ->
                    if (card.cardId == cardId) regeneratedCard else card
                }
            )
        }
    }

    private suspend fun runPreviewUpdate(
        state: NewCardUiState,
        transform: suspend (current: NewCardUiState, preview: GeneratedLearningNote) -> GeneratedLearningNote,
    ): NewCardPreviewUpdateResult {
        val preview = state.learningNotePreview ?: return NewCardPreviewUpdateResult.NoPreview

        return runCatching {
            transform(state, preview)
        }.fold(
            onSuccess = { updatedPreview ->
                val previewValidation = generationDependencies.validateGeneratedLearningNoteUseCase(updatedPreview)
                NewCardPreviewUpdateResult.Updated(preview = updatedPreview, validation = previewValidation)
            },
            onFailure = { error ->
                if (error is DomainValidationException) {
                    NewCardPreviewUpdateResult.DomainError(error.issues)
                } else {
                    NewCardPreviewUpdateResult.UnexpectedError(error)
                }
            },
        )
    }
}

sealed interface NewCardPreviewResult {
    data class InputInvalid(
        val errors: List<com.emm.domain.validation.ValidationIssue>,
        val warnings: List<com.emm.domain.validation.ValidationIssue>,
    ) : NewCardPreviewResult

    data class PreviewReady(
        val preview: GeneratedLearningNote,
        val validation: com.emm.domain.validation.ValidationResult<GeneratedLearningNote>,
    ) : NewCardPreviewResult

    data class DomainError(
        val issues: List<com.emm.domain.validation.ValidationIssue>,
    ) : NewCardPreviewResult

    data class UnexpectedError(
        val error: Throwable,
    ) : NewCardPreviewResult
}

sealed interface NewCardPreviewUpdateResult {
    data object NoPreview : NewCardPreviewUpdateResult

    data class Updated(
        val preview: GeneratedLearningNote,
        val validation: com.emm.domain.validation.ValidationResult<GeneratedLearningNote>,
    ) : NewCardPreviewUpdateResult

    data class DomainError(
        val issues: List<com.emm.domain.validation.ValidationIssue>,
    ) : NewCardPreviewUpdateResult

    data class UnexpectedError(
        val error: Throwable,
    ) : NewCardPreviewUpdateResult
}

package com.emm.hello.newfeatures.card

import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase

class NewCardDraftEditor(
    private val validateGeneratedLearningNoteUseCase: ValidateGeneratedLearningNoteUseCase,
) {

    fun editField(
        state: NewCardUiState,
        field: EditableLearningNoteField,
        value: String,
    ): NewCardUiState {
        return updatePreview(state) { note ->
            note.withEditedField(field = field, value = value)
        }
    }

    fun editCardPrompt(
        state: NewCardUiState,
        cardId: String,
        prompt: String,
    ): NewCardUiState {
        return updatePreviewCard(state, cardId) { copy(prompt = prompt) }
    }

    fun editCardExpectedAnswer(
        state: NewCardUiState,
        cardId: String,
        expectedAnswer: String,
    ): NewCardUiState {
        return updatePreviewCard(state, cardId) { copy(expectedAnswer = expectedAnswer) }
    }

    fun editCardHint(
        state: NewCardUiState,
        cardId: String,
        hint: String,
    ): NewCardUiState {
        return updatePreviewCard(state, cardId) { copy(hint = hint) }
    }

    fun editCardActive(
        state: NewCardUiState,
        cardId: String,
        isActive: Boolean,
    ): NewCardUiState {
        return updatePreviewCard(state, cardId) { copy(isActive = isActive) }
    }

    fun applyUpdatedPreview(
        state: NewCardUiState,
        updatedPreview: GeneratedLearningNote,
    ): NewCardUiState {
        val previewValidation = validateGeneratedLearningNoteUseCase(updatedPreview)
        return state.withPreviewValidation(updatedPreview, previewValidation)
    }

    private fun updatePreview(
        state: NewCardUiState,
        transform: (GeneratedLearningNote) -> GeneratedLearningNote,
    ): NewCardUiState {
        val currentPreview = state.learningNotePreview ?: return state
        return applyUpdatedPreview(state, transform(currentPreview))
    }

    private fun updatePreviewCard(
        state: NewCardUiState,
        cardId: String,
        transform: GeneratedStudyCard.() -> GeneratedStudyCard,
    ): NewCardUiState {
        return updatePreview(state) { note ->
            note.copy(
                cards = note.cards.map { card ->
                    if (card.cardId == cardId) card.transform() else card
                }
            )
        }
    }
}

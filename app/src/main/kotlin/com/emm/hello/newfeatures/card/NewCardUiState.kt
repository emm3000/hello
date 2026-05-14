package com.emm.hello.newfeatures.card

import com.emm.domain.deck.Deck
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.data.catalog.StaticCategories
import com.emm.data.catalog.difficult
import com.emm.data.catalog.staticCategories
import com.emm.domain.validation.ValidationIssue
import com.emm.hello.core.mvi.MviState

sealed interface PreviewRegenerationTarget {
    data object Example : PreviewRegenerationTarget
    data object Cloze : PreviewRegenerationTarget
    data class Field(val field: EditableLearningNoteField) : PreviewRegenerationTarget
    data class Card(val cardId: String) : PreviewRegenerationTarget
}

data class NewCardUiState(
    val word: String = "",
    val aiRequest: String = "",
    val intendedMeaningEs: String = "",
    val contextSentence: String = "",
    val decks: List<Deck> = emptyList(),
    val deckSelected: Deck? = null,
    val isLoading: Boolean = false,
    val isCheck: Boolean = false,
    val category: StaticCategories = staticCategories.first(),
    val typeView: TypeView = TypeView.WordOrPhase,
    val difficulty: String = difficult.first(),
    val error: NewCardErrorUi? = null,
    val inputValidationIssues: List<ValidationIssue> = emptyList(),
    val inputWarningIssues: List<ValidationIssue> = emptyList(),
    val learningNotePreview: GeneratedLearningNote? = null,
    val previewGeneratedWarnings: List<String> = emptyList(),
    val previewValidationIssues: List<ValidationIssue> = emptyList(),
    val previewWarningIssues: List<ValidationIssue> = emptyList(),
    val canSavePreview: Boolean = false,
    val previewRegenerationTarget: PreviewRegenerationTarget? = null,
) : MviState

package com.emm.hello.newfeatures.card

import com.emm.domain.deck.Deck
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.catalog.StaticCategories
import com.emm.domain.catalog.difficult
import com.emm.domain.catalog.staticCategories
import com.emm.domain.validation.ValidationIssue
import com.emm.hello.core.mvi.MviState

private const val QUOTA_LOW_THRESHOLD = 10

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
    val isSetAsDefault: Boolean = false,
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
    val quotaRemaining: Int = Int.MAX_VALUE,
) : MviState {
    val showQuotaWarning: Boolean
        get() = quotaRemaining in 1..QUOTA_LOW_THRESHOLD
}

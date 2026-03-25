package com.emm.hello.newfeatures.card

import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.GeneratedLearningNote
import com.emm.domain.flashcard.GeneratedLearningNoteIssue
import com.emm.domain.flashcard.StaticCategories
import com.emm.domain.flashcard.TypeView
import com.emm.domain.flashcard.difficult
import com.emm.domain.flashcard.staticCategories

sealed interface PreviewRegenerationTarget {
    data object Example : PreviewRegenerationTarget
    data object Cloze : PreviewRegenerationTarget
    data class Field(val field: EditableLearningNoteField) : PreviewRegenerationTarget
    data class Card(val cardId: String) : PreviewRegenerationTarget
}

data class NewCardUiState(
    val word: String = "",
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
    val learningNotePreview: GeneratedLearningNote? = null,
    val previewValidationErrors: List<String> = emptyList(),
    val previewWarnings: List<String> = emptyList(),
    val previewValidationIssues: List<GeneratedLearningNoteIssue> = emptyList(),
    val previewWarningIssues: List<GeneratedLearningNoteIssue> = emptyList(),
    val canSavePreview: Boolean = false,
    val previewRegenerationTarget: PreviewRegenerationTarget? = null,
)

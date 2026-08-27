package com.emm.hello.newfeatures.card

import com.emm.domain.deck.Deck
import com.emm.hello.core.mvi.MviIntent

enum class EditableLearningNoteField {
    IntendedMeaningEs,
    SimpleDefinitionEn,
    WhyUseful,
    ExampleSentence,
    ExampleTranslation,
    UsagePattern,
    CommonMistake,
    ClozeSentence,
}

sealed interface NewCardUiIntent : MviIntent {

    data class WordChanged(val word: String) : NewCardUiIntent

    data class IntendedMeaningChanged(val intendedMeaningEs: String) : NewCardUiIntent

    data class ContextSentenceChanged(val contextSentence: String) : NewCardUiIntent

    data class DeckSelected(val deck: Deck) : NewCardUiIntent

    data class CheckChanged(val checked: Boolean) : NewCardUiIntent

    data class DifficultySelected(val difficulty: String) : NewCardUiIntent

    data class PreviewFieldChanged(
        val field: EditableLearningNoteField,
        val value: String,
    ) : NewCardUiIntent

    data class PreviewCardPromptChanged(
        val cardId: String,
        val prompt: String,
    ) : NewCardUiIntent

    data class PreviewCardExpectedAnswerChanged(
        val cardId: String,
        val expectedAnswer: String,
    ) : NewCardUiIntent

    data class PreviewCardHintChanged(
        val cardId: String,
        val hint: String,
    ) : NewCardUiIntent

    data class PreviewCardActiveChanged(
        val cardId: String,
        val isActive: Boolean,
    ) : NewCardUiIntent

    data object RegenerateExampleClicked : NewCardUiIntent

    data object RegenerateClozeClicked : NewCardUiIntent

    data class RegenerateFieldClicked(
        val field: EditableLearningNoteField,
    ) : NewCardUiIntent

    data class RegenerateCardClicked(
        val cardId: String,
    ) : NewCardUiIntent

    data object GenerateClicked : NewCardUiIntent

    data object SaveClicked : NewCardUiIntent
}

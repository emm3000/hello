package com.emm.hello.newfeatures.capture

import com.emm.domain.ids.DeckId
import com.emm.hello.core.mvi.MviIntent

sealed interface CaptureUiIntent : MviIntent {

    data class WordChanged(val word: String) : CaptureUiIntent
    data object ManualModeToggled : CaptureUiIntent
    data class TranslationChanged(val translation: String) : CaptureUiIntent
    data class MeaningChanged(val meaning: String) : CaptureUiIntent
    data object Submit : CaptureUiIntent
    data object RetryFailed : CaptureUiIntent
    data object DeckPickerOpened : CaptureUiIntent
    data object DeckPickerDismissed : CaptureUiIntent
    data class DeckSelected(val deckId: DeckId) : CaptureUiIntent
}

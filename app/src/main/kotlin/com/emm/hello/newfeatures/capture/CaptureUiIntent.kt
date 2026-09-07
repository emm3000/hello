package com.emm.hello.newfeatures.capture

import com.emm.hello.core.mvi.MviIntent

sealed interface CaptureUiIntent : MviIntent {

    data class WordChanged(val word: String) : CaptureUiIntent
    data object ManualModeToggled : CaptureUiIntent
    data class TranslationChanged(val translation: String) : CaptureUiIntent
    data class MeaningChanged(val meaning: String) : CaptureUiIntent
    data object Submit : CaptureUiIntent
    data object RetryFailed : CaptureUiIntent
}

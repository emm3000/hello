package com.emm.hello.newfeatures.capture

import androidx.annotation.StringRes
import com.emm.hello.core.mvi.MviEffect

sealed interface CaptureUiEffect : MviEffect {

    data class ShowMessage(@StringRes val messageRes: Int) : CaptureUiEffect
    data class EnqueueEnrichment(val flashcardIds: List<String>) : CaptureUiEffect
}

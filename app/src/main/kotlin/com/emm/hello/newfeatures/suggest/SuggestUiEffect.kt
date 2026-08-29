package com.emm.hello.newfeatures.suggest

import androidx.annotation.StringRes
import com.emm.hello.core.mvi.MviEffect

sealed interface SuggestUiEffect : MviEffect {

    data class EnqueueEnrichment(val flashcardIds: List<String>) : SuggestUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : SuggestUiEffect
    data object NavigateBack : SuggestUiEffect
}

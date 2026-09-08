package com.emm.hello.newfeatures.store

import androidx.annotation.StringRes
import com.emm.hello.core.mvi.MviEffect

sealed interface StoreUiEffect : MviEffect {
    data object NavigateBack : StoreUiEffect
    data class OpenDeck(val deckId: String) : StoreUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : StoreUiEffect
}

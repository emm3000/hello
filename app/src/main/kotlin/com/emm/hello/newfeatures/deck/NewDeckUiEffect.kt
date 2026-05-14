package com.emm.hello.newfeatures.deck

import com.emm.hello.core.mvi.MviEffect

sealed interface NewDeckUiEffect : MviEffect {
    data object NavigateBack : NewDeckUiEffect
    data class ShowMessage(val message: String) : NewDeckUiEffect
}

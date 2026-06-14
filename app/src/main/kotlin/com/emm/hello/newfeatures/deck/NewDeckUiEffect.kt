package com.emm.hello.newfeatures.deck

import androidx.annotation.StringRes
import com.emm.hello.core.mvi.MviEffect

sealed interface NewDeckUiEffect : MviEffect {
    data object NavigateBack : NewDeckUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : NewDeckUiEffect
}

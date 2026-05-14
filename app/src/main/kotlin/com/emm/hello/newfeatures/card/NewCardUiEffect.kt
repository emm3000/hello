package com.emm.hello.newfeatures.card

import com.emm.hello.core.mvi.MviEffect

sealed interface NewCardUiEffect : MviEffect {
    data class ShowMessage(val message: String) : NewCardUiEffect
    data object OpenReview : NewCardUiEffect
    data object CloseFlow : NewCardUiEffect
}

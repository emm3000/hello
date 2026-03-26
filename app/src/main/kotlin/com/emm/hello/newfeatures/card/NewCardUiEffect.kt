package com.emm.hello.newfeatures.card

sealed interface NewCardUiEffect {
    data class ShowMessage(val message: String) : NewCardUiEffect
    data object OpenReview : NewCardUiEffect
    data object CloseFlow : NewCardUiEffect
}

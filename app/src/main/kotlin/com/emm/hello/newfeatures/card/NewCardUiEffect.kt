package com.emm.hello.newfeatures.card

sealed interface NewCardUiEffect {
    data class ShowMessage(val message: String) : NewCardUiEffect
}

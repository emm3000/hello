package com.emm.hello.newfeatures.deck

sealed interface NewDeckUiEffect {
    data object NavigateBack : NewDeckUiEffect
    data class ShowMessage(val message: String) : NewDeckUiEffect
}

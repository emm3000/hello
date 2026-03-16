package com.emm.hello.newfeatures.pairing

sealed interface PairingUiEffect {
    data class ShowMessage(val message: String) : PairingUiEffect
}

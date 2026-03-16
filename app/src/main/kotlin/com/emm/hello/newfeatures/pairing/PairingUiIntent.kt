package com.emm.hello.newfeatures.pairing

sealed interface PairingUiIntent {
    data class JoinCodeChanged(val value: String) : PairingUiIntent
    data object RefreshDevicesClicked : PairingUiIntent
    data object CreateCodeClicked : PairingUiIntent
    data object JoinWithCodeClicked : PairingUiIntent
    data class RevokeDeviceClicked(val deviceId: String) : PairingUiIntent
}

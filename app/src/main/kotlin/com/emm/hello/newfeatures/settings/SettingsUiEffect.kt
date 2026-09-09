package com.emm.hello.newfeatures.settings

import androidx.annotation.StringRes
import com.emm.hello.core.mvi.MviEffect

sealed interface SettingsUiEffect : MviEffect {
    data class ShowSuccess(@StringRes val messageRes: Int, val formatArg: String? = null) : SettingsUiEffect
    data class ShowError(@StringRes val messageRes: Int) : SettingsUiEffect
    data object LaunchExportPicker : SettingsUiEffect
    data object LaunchImportPicker : SettingsUiEffect
    data object RequestNotificationPermission : SettingsUiEffect
    data object OpenNotificationSettings : SettingsUiEffect
    data class CopyToClipboard(val text: String) : SettingsUiEffect
}

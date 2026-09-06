package com.emm.hello.newfeatures.settings

import com.emm.hello.core.mvi.MviEffect

sealed interface SettingsUiEffect : MviEffect {
    data class ShowSuccess(val message: String) : SettingsUiEffect
    data class ShowError(val message: String) : SettingsUiEffect
    data object LaunchExportPicker : SettingsUiEffect
    data object LaunchImportPicker : SettingsUiEffect
    data object RequestNotificationPermission : SettingsUiEffect
    data object OpenNotificationSettings : SettingsUiEffect
}

package com.emm.hello.newfeatures.settings

/**
 * MVI Effects for the Settings screen.
 * Effects are one-shot events consumed by the UI.
 */
sealed interface SettingsUiEffect {
    data class ShowSuccess(val message: String) : SettingsUiEffect
    data class ShowError(val message: String) : SettingsUiEffect
}

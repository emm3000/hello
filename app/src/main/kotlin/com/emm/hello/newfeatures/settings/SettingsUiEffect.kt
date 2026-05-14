package com.emm.hello.newfeatures.settings

import com.emm.hello.core.mvi.MviEffect

/**
 * MVI Effects for the Settings screen.
 * Effects are one-shot events consumed by the UI.
 */
sealed interface SettingsUiEffect : MviEffect {
    data class ShowSuccess(val message: String) : SettingsUiEffect
    data class ShowError(val message: String) : SettingsUiEffect
}

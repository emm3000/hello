package com.emm.hello.newfeatures.settings

import com.emm.hello.core.mvi.MviIntent

/**
 * MVI Intents for the Settings screen.
 * ExportData — triggers SAF file picker for export
 * ImportData — triggers SAF file picker for import
 * ConfirmImport — user confirmed the destructive dialog
 * CancelImport — user dismissed the dialog
 */
sealed interface SettingsUiIntent : MviIntent {
    data object ExportData : SettingsUiIntent
    data object ImportData : SettingsUiIntent
    data object ConfirmImport : SettingsUiIntent
    data object CancelImport : SettingsUiIntent
}

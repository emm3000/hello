package com.emm.hello.newfeatures.settings

import android.net.Uri
import com.emm.hello.core.mvi.MviIntent

sealed interface SettingsUiIntent : MviIntent {
    data object ExportData : SettingsUiIntent
    data object ImportData : SettingsUiIntent
    data object ConfirmImport : SettingsUiIntent
    data object CancelImport : SettingsUiIntent
    data class ExportUriReceived(val uri: Uri) : SettingsUiIntent
    data class ImportUriReceived(val uri: Uri) : SettingsUiIntent
    data class SetReminderEnabled(val isEnabled: Boolean) : SettingsUiIntent
}

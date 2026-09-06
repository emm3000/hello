package com.emm.hello.newfeatures.settings

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.emm.data.export.BackupExporter
import com.emm.data.export.BackupImporter
import com.emm.data.export.IncompatibleSchemaException
import com.emm.domain.reminder.GetStudyReminderSettingsUseCase
import com.emm.domain.reminder.SetStudyReminderEnabledUseCase
import com.emm.domain.reminder.SetStudyReminderTimeUseCase
import com.emm.domain.reminder.StudyReminderSettings
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import com.emm.hello.notifications.NotificationPermission
import java.time.LocalTime
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"

class SettingsViewModel(
    private val exportDataSource: BackupExporter,
    private val importDataSource: BackupImporter,
    private val getStudyReminderSettings: GetStudyReminderSettingsUseCase,
    private val setStudyReminderEnabled: SetStudyReminderEnabledUseCase,
    private val setStudyReminderTime: SetStudyReminderTimeUseCase,
    private val notificationPermission: NotificationPermission,
) : MviViewModel<SettingsUiState, SettingsUiIntent, SettingsUiEffect>(
    initialState = SettingsUiState(),
) {

    init {
        val settings: StudyReminderSettings = getStudyReminderSettings()
        setState {
            copy(
                isReminderEnabled = settings.isEnabled,
                reminderTime = settings.time,
                isNotificationPermissionGranted = notificationPermission.isGranted(),
            )
        }
    }

    override fun onIntent(intent: SettingsUiIntent) {
        when (intent) {
            is SettingsUiIntent.ExportData -> sendEffect(SettingsUiEffect.LaunchExportPicker)
            is SettingsUiIntent.ImportData -> sendEffect(SettingsUiEffect.LaunchImportPicker)
            is SettingsUiIntent.ExportUriReceived -> exportToUri(intent.uri)
            is SettingsUiIntent.ImportUriReceived -> setState {
                copy(isConfirmDialogVisible = true, pendingImportUri = intent.uri)
            }
            is SettingsUiIntent.ConfirmImport -> confirmImport()
            is SettingsUiIntent.CancelImport -> cancelImport()
            is SettingsUiIntent.SetReminderEnabled -> setReminderEnabled(intent.isEnabled)
            is SettingsUiIntent.EditReminderTime -> setState { copy(isReminderTimePickerVisible = true) }
            is SettingsUiIntent.DismissReminderTimePicker -> setState { copy(isReminderTimePickerVisible = false) }
            is SettingsUiIntent.SetReminderTime -> setReminderTime(intent.time)
            is SettingsUiIntent.NotificationPermissionSettled -> notificationPermissionSettled()
            is SettingsUiIntent.RefreshNotificationPermission -> refreshNotificationPermission()
            is SettingsUiIntent.OpenNotificationSettings -> sendEffect(SettingsUiEffect.OpenNotificationSettings)
        }
    }

    private fun setReminderEnabled(isEnabled: Boolean) {
        if (!isEnabled) {
            setStudyReminderEnabled(false)
            setState { copy(isReminderEnabled = false) }
            return
        }
        if (!notificationPermission.isGranted()) {
            sendEffect(SettingsUiEffect.RequestNotificationPermission)
            return
        }
        setStudyReminderEnabled(true)
        setState { copy(isReminderEnabled = true, isNotificationPermissionGranted = true) }
    }

    private fun notificationPermissionSettled() {
        val isGranted: Boolean = notificationPermission.isGranted()
        setState { copy(isNotificationPermissionGranted = isGranted) }
        if (isGranted) {
            setStudyReminderEnabled(true)
            setState { copy(isReminderEnabled = true) }
        }
    }

    private fun refreshNotificationPermission() {
        val isGranted: Boolean = notificationPermission.isGranted()
        setState { copy(isNotificationPermissionGranted = isGranted) }
    }

    private fun setReminderTime(time: LocalTime) {
        setStudyReminderTime(time)
        setState { copy(reminderTime = time, isReminderTimePickerVisible = false) }
    }

    private fun exportToUri(uri: Uri) {
        viewModelScope.launch {
            setState { copy(isExporting = true) }
            exportDataSource.export(uri)
                .onSuccess {
                    sendEffect(SettingsUiEffect.ShowSuccess("Backup exported successfully"))
                }
                .onFailure { error ->
                    logError(TAG, "export:error ${error.message}", error)
                    sendEffect(SettingsUiEffect.ShowError("Couldn't export the backup"))
                }
            setState { copy(isExporting = false) }
        }
    }

    private fun confirmImport() {
        val uri = currentState.pendingImportUri ?: return
        setState { copy(isConfirmDialogVisible = false) }

        viewModelScope.launch {
            setState { copy(isImporting = true) }
            importDataSource.import(uri)
                .onSuccess {
                    sendEffect(SettingsUiEffect.ShowSuccess("Backup restored"))
                }
                .onFailure { error ->
                    logError(TAG, "import:error ${error.message}", error)
                    sendEffect(SettingsUiEffect.ShowError(humanizeImportError(error)))
                }
            setState { copy(isImporting = false, pendingImportUri = null) }
        }
    }

    private fun cancelImport() {
        setState { copy(isConfirmDialogVisible = false, pendingImportUri = null) }
    }

    private fun humanizeImportError(error: Throwable): String = when {
        error is IncompatibleSchemaException || hasCause<IncompatibleSchemaException>(error) ->
            "This backup was created with another version of the app. Update the app and try again."
        else -> "Couldn't restore the backup."
    }

    private inline fun <reified T : Throwable> hasCause(error: Throwable): Boolean {
        var current: Throwable? = error.cause
        while (current != null && current !== error) {
            if (current is T) return true
            current = current.cause
        }
        return false
    }
}

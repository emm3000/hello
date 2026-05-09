package com.emm.hello.newfeatures.settings

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.emm.data.export.BackupExporter
import com.emm.data.export.BackupImporter
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"

class SettingsViewModel(
    private val exportDataSource: BackupExporter,
    private val importDataSource: BackupImporter,
) : MviViewModel<SettingsUiState, SettingsUiIntent, SettingsUiEffect>(
    initialState = SettingsUiState(),
) {

    override fun onIntent(intent: SettingsUiIntent) {
        when (intent) {
            is SettingsUiIntent.ExportData -> { /* SAF picker handled by Route */ }
            is SettingsUiIntent.ImportData -> { /* handled by Route SAF launcher */ }
            is SettingsUiIntent.ConfirmImport -> confirmImport()
            is SettingsUiIntent.CancelImport -> cancelImport()
        }
    }

    fun onExportUri(uri: Uri) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isExporting = true)
            exportDataSource.export(uri)
                .onSuccess {
                    mutableEffect.send(SettingsUiEffect.ShowSuccess("Backup exported successfully"))
                }
                .onFailure { error ->
                    logError(TAG, "export:error ${error.message}", error)
                    mutableEffect.send(SettingsUiEffect.ShowError(error.message ?: "Export failed"))
                }
            mutableState.value = mutableState.value.copy(isExporting = false)
        }
    }

    fun onImportUri(uri: Uri) {
        mutableState.value = mutableState.value.copy(
            showConfirmDialog = true,
            pendingImportUri = uri,
        )
    }

    private fun confirmImport() {
        val uri = mutableState.value.pendingImportUri ?: return
        mutableState.value = mutableState.value.copy(showConfirmDialog = false)

        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isImporting = true)
            importDataSource.import(uri)
                .onSuccess {
                    mutableEffect.send(SettingsUiEffect.ShowSuccess("Backup restored successfully"))
                }
                .onFailure { error ->
                    logError(TAG, "import:error ${error.message}", error)
                    mutableEffect.send(SettingsUiEffect.ShowError(error.message ?: "Import failed"))
                }
            mutableState.value = mutableState.value.copy(isImporting = false, pendingImportUri = null)
        }
    }

    private fun cancelImport() {
        mutableState.value = mutableState.value.copy(
            showConfirmDialog = false,
            pendingImportUri = null,
        )
    }
}

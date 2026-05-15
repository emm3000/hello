package com.emm.hello.newfeatures.settings

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.emm.data.export.BackupExporter
import com.emm.data.export.BackupImporter
import com.emm.data.export.IncompatibleSchemaException
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
            setState { copy(isExporting = true) }
            exportDataSource.export(uri)
                .onSuccess {
                    sendEffect(SettingsUiEffect.ShowSuccess("Backup exported successfully"))
                }
                .onFailure { error ->
                    logError(TAG, "export:error ${error.message}", error)
                    sendEffect(SettingsUiEffect.ShowError(error.message ?: "Export failed"))
                }
            setState { copy(isExporting = false) }
        }
    }

    fun onImportUri(uri: Uri) {
        setState { copy(isConfirmDialogVisible = true, pendingImportUri = uri) }
    }

    private fun confirmImport() {
        val uri = currentState.pendingImportUri ?: return
        setState { copy(isConfirmDialogVisible = false) }

        viewModelScope.launch {
            setState { copy(isImporting = true) }
            importDataSource.import(uri)
                .onSuccess {
                    sendEffect(SettingsUiEffect.ShowSuccess("Backup restored successfully"))
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
            "Este backup fue creado con otra versión de la app. Actualizá la app e intentá de nuevo."
        else -> error.message ?: "No se pudo restaurar el backup."
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

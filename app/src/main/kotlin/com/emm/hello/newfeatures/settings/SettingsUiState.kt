package com.emm.hello.newfeatures.settings

import android.net.Uri

/**
 * MVI State for the Settings screen.
 * Holds loading states for export/import operations, confirmation dialog visibility,
 * and the pending import URI (MVI-compliant: all state in one place).
 */
data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val pendingImportUri: Uri? = null,
)

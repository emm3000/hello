package com.emm.hello.newfeatures.settings

/**
 * MVI State for the Settings screen.
 * Holds loading states for export/import operations and confirmation dialog visibility.
 */
data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val showConfirmDialog: Boolean = false,
)

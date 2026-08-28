package com.emm.hello.newfeatures.settings

import android.net.Uri
import com.emm.hello.core.mvi.MviState

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isConfirmDialogVisible: Boolean = false,
    val pendingImportUri: Uri? = null,
) : MviState

package com.emm.hello.newfeatures.settings

import android.net.Uri
import com.emm.domain.account.Account
import com.emm.domain.generation.GenerationCredits
import com.emm.domain.reminder.StudyReminderSettings
import com.emm.hello.core.mvi.MviState
import java.time.LocalTime

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isConfirmDialogVisible: Boolean = false,
    val pendingImportUri: Uri? = null,
    val isReminderEnabled: Boolean = true,
    val reminderTime: LocalTime = StudyReminderSettings.DEFAULT_TIME,
    val isReminderTimePickerVisible: Boolean = false,
    val isNotificationPermissionGranted: Boolean = true,
    val account: Account? = null,
    val isLinkingAccount: Boolean = false,
    val generationCredits: GenerationCredits? = null,
    val buildInfo: BuildInfo = BuildInfo(versionName = "", versionCode = 0, commit = ""),
) : MviState

package com.emm.hello.newfeatures.settings

import android.net.Uri
import com.emm.domain.account.Account
import com.emm.domain.dailywords.DailyWordsSettings
import com.emm.domain.generation.GenerationCredits
import com.emm.domain.reminder.StudyReminderSettings
import com.emm.domain.study.DailyNewCardLimit
import com.emm.hello.core.mvi.MviState
import java.time.DayOfWeek
import java.time.LocalTime

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isConfirmDialogVisible: Boolean = false,
    val pendingImportUri: Uri? = null,
    val isReminderEnabled: Boolean = true,
    val reminderTime: LocalTime = StudyReminderSettings.DEFAULT_TIME,
    val dailyNewCardLimit: DailyNewCardLimit = DailyNewCardLimit.DEFAULT,
    val isReminderTimePickerVisible: Boolean = false,
    val isDailyWordsEnabled: Boolean = DailyWordsSettings.DEFAULT.isEnabled,
    val dailyWordsTime: LocalTime = DailyWordsSettings.DEFAULT_TIME,
    val dailyWordsDays: Set<DayOfWeek> = DailyWordsSettings.DEFAULT_DAYS,
    val isDailyWordsTimePickerVisible: Boolean = false,
    val pendingNotificationPermission: NotificationPermissionTarget? = null,
    val isNotificationPermissionGranted: Boolean = true,
    val account: Account? = null,
    val isLinkingAccount: Boolean = false,
    val generationCredits: GenerationCredits? = null,
    val buildInfo: BuildInfo = BuildInfo(versionName = "", versionCode = 0, commit = ""),
) : MviState

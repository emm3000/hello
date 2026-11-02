package com.emm.hello.newfeatures.settings

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.emm.data.export.BackupExporter
import com.emm.data.export.BackupImporter
import com.emm.data.export.IncompatibleSchemaException
import com.emm.domain.account.Account
import com.emm.domain.account.AccountLinkResult
import com.emm.domain.account.GetAccountUseCase
import com.emm.domain.account.LinkGoogleAccountUseCase
import com.emm.domain.dailywords.DailyWordsSettings
import com.emm.domain.dailywords.GetDailyWordsSettingsUseCase
import com.emm.domain.dailywords.SetDailyWordsEnabledUseCase
import com.emm.domain.dailywords.SetDailyWordsTimeUseCase
import com.emm.domain.dailywords.ToggleDailyWordsDayUseCase
import com.emm.domain.generation.GenerationCredits
import com.emm.domain.generation.GenerationCreditsRepository
import com.emm.domain.generation.isFreshAt
import com.emm.domain.reminder.GetStudyReminderSettingsUseCase
import com.emm.domain.reminder.SetStudyReminderEnabledUseCase
import com.emm.domain.reminder.SetStudyReminderTimeUseCase
import com.emm.domain.reminder.StudyReminderSettings
import com.emm.domain.study.DailyNewCardLimit
import com.emm.domain.study.DailyNewCardLimitRepository
import com.emm.domain.time.Clock
import com.emm.hello.R
import com.emm.hello.analytics.ProductAnalytics
import com.emm.hello.analytics.ProductEvent
import com.emm.hello.core.auth.GoogleSignInLauncher
import com.emm.hello.core.auth.GoogleSignInResult
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import com.emm.hello.notifications.NotificationPermission
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"

@Suppress("LongParameterList")
class SettingsViewModel(
    private val exportDataSource: BackupExporter,
    private val importDataSource: BackupImporter,
    private val getStudyReminderSettings: GetStudyReminderSettingsUseCase,
    private val setStudyReminderEnabled: SetStudyReminderEnabledUseCase,
    private val setStudyReminderTime: SetStudyReminderTimeUseCase,
    getDailyWordsSettings: GetDailyWordsSettingsUseCase,
    private val setDailyWordsEnabledUseCase: SetDailyWordsEnabledUseCase,
    private val setDailyWordsTimeUseCase: SetDailyWordsTimeUseCase,
    private val toggleDailyWordsDayUseCase: ToggleDailyWordsDayUseCase,
    private val dailyNewCardLimit: DailyNewCardLimitRepository,
    private val notificationPermission: NotificationPermission,
    private val getAccount: GetAccountUseCase,
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase,
    private val googleSignInLauncher: GoogleSignInLauncher,
    private val googleServerClientId: String,
    private val generationCredits: GenerationCreditsRepository,
    private val clock: Clock,
    buildInfo: BuildInfo,
    private val productAnalytics: ProductAnalytics,
) : MviViewModel<SettingsUiState, SettingsUiIntent, SettingsUiEffect>(
    initialState = SettingsUiState(buildInfo = buildInfo),
) {

    init {
        val settings: StudyReminderSettings = getStudyReminderSettings()
        val newCardLimit: DailyNewCardLimit = dailyNewCardLimit.get()
        val dailyWordsSettings: DailyWordsSettings = getDailyWordsSettings()
        setState {
            copy(
                isReminderEnabled = settings.isEnabled,
                reminderTime = settings.time,
                isDailyWordsEnabled = dailyWordsSettings.isEnabled,
                dailyWordsTime = dailyWordsSettings.time,
                dailyWordsDays = dailyWordsSettings.days,
                dailyNewCardLimit = newCardLimit,
                isNotificationPermissionGranted = notificationPermission.isGranted(),
            )
        }
        loadAccount()
        observeGenerationCredits()
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
            is SettingsUiIntent.SetDailyWordsEnabled -> setDailyWordsEnabled(intent.isEnabled)
            is SettingsUiIntent.EditDailyWordsTime -> setState { copy(isDailyWordsTimePickerVisible = true) }
            is SettingsUiIntent.DismissDailyWordsTimePicker -> setState { copy(isDailyWordsTimePickerVisible = false) }
            is SettingsUiIntent.SetDailyWordsTime -> setDailyWordsTime(intent.time)
            is SettingsUiIntent.ToggleDailyWordsDay -> toggleDailyWordsDay(intent.day)
            is SettingsUiIntent.DailyNewCardLimitSelected -> selectDailyNewCardLimit(intent.limit)
            is SettingsUiIntent.NotificationPermissionSettled -> notificationPermissionSettled()
            is SettingsUiIntent.RefreshNotificationPermission -> refreshNotificationPermission()
            is SettingsUiIntent.OpenNotificationSettings -> sendEffect(SettingsUiEffect.OpenNotificationSettings)
            is SettingsUiIntent.LinkGoogleAccount -> requestGoogleSignIn()
            is SettingsUiIntent.CopyBuildInfo ->
                sendEffect(SettingsUiEffect.CopyToClipboard(currentState.buildInfo.label))
        }
    }

    private fun loadAccount() {
        viewModelScope.launch {
            val account: Account? = getAccount()
            setState { copy(account = account) }
        }
    }

    private fun observeGenerationCredits() {
        viewModelScope.launch {
            generationCredits.observe().collect { credits: GenerationCredits? ->
                val fresh: GenerationCredits? = credits?.takeIf { it.isFreshAt(clock.now()) }
                setState { copy(generationCredits = fresh) }
            }
        }
    }

    private fun requestGoogleSignIn() {
        if (isAccountLinked()) return
        setState { copy(isLinkingAccount = true) }
        viewModelScope.launch {
            when (val result: GoogleSignInResult = googleSignInLauncher.signIn(googleServerClientId)) {
                is GoogleSignInResult.Success -> linkGoogleAccount(result.idToken, result.rawNonce)
                GoogleSignInResult.Cancelled -> setState { copy(isLinkingAccount = false) }
                GoogleSignInResult.NoCredentials -> googleLinkFailed(R.string.settings_google_no_credentials)
                is GoogleSignInResult.Failure -> googleLinkFailed(R.string.settings_google_link_failed)
            }
        }
    }

    private fun linkGoogleAccount(idToken: String, rawNonce: String) {
        viewModelScope.launch {
            try {
                val result: AccountLinkResult = linkGoogleAccountUseCase(idToken = idToken, rawNonce = rawNonce)
                setState { copy(account = result.account, isLinkingAccount = false) }
                sendEffect(linkSuccessEffect(result))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                logError(TAG, "linkGoogleAccount:error ${error.message}", error)
                setState { copy(isLinkingAccount = false) }
                sendEffect(SettingsUiEffect.ShowError(R.string.settings_google_link_failed))
            }
        }
    }

    private fun isAccountLinked(): Boolean = currentState.account?.isAnonymous == false

    private fun linkSuccessEffect(result: AccountLinkResult): SettingsUiEffect.ShowSuccess = when (result) {
        is AccountLinkResult.Linked -> SettingsUiEffect.ShowSuccess(R.string.settings_google_link_success)
        is AccountLinkResult.SwitchedToExisting -> signedInEffect(result.account.email)
    }

    private fun signedInEffect(email: String?): SettingsUiEffect.ShowSuccess =
        if (email == null) {
            SettingsUiEffect.ShowSuccess(R.string.settings_google_link_success)
        } else {
            SettingsUiEffect.ShowSuccess(R.string.settings_google_signed_in_as, email)
        }

    private fun googleLinkFailed(@StringRes messageRes: Int) {
        setState { copy(isLinkingAccount = false) }
        sendEffect(SettingsUiEffect.ShowError(messageRes))
    }

    private fun setReminderEnabled(isEnabled: Boolean) {
        if (!isEnabled) {
            setStudyReminderEnabled(false)
            setState { copy(isReminderEnabled = false) }
            return
        }
        if (!notificationPermission.isGranted()) {
            requestNotificationPermission(NotificationPermissionTarget.StudyReminder)
            return
        }
        setStudyReminderEnabled(true)
        setState { copy(isReminderEnabled = true, isNotificationPermissionGranted = true) }
    }

    private fun setDailyWordsEnabled(isEnabled: Boolean) {
        if (!isEnabled) {
            setDailyWordsEnabledUseCase(false)
            setState { copy(isDailyWordsEnabled = false) }
            return
        }
        if (!notificationPermission.isGranted()) {
            requestNotificationPermission(NotificationPermissionTarget.DailyWords)
            return
        }
        setDailyWordsEnabledUseCase(true)
        setState { copy(isDailyWordsEnabled = true, isNotificationPermissionGranted = true) }
    }

    private fun requestNotificationPermission(target: NotificationPermissionTarget) {
        setState { copy(pendingNotificationPermission = target) }
        sendEffect(SettingsUiEffect.RequestNotificationPermission)
    }

    private fun notificationPermissionSettled() {
        val isGranted: Boolean = notificationPermission.isGranted()
        val target: NotificationPermissionTarget? = currentState.pendingNotificationPermission
        setState { copy(isNotificationPermissionGranted = isGranted, pendingNotificationPermission = null) }
        if (isGranted) enableAfterPermission(target)
    }

    private fun enableAfterPermission(target: NotificationPermissionTarget?) {
        when (target) {
            NotificationPermissionTarget.StudyReminder -> {
                setStudyReminderEnabled(true)
                setState { copy(isReminderEnabled = true) }
            }
            NotificationPermissionTarget.DailyWords -> {
                setDailyWordsEnabledUseCase(true)
                setState { copy(isDailyWordsEnabled = true) }
            }
            null -> Unit
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

    private fun setDailyWordsTime(time: LocalTime) {
        setDailyWordsTimeUseCase(time)
        setState { copy(dailyWordsTime = time, isDailyWordsTimePickerVisible = false) }
    }

    private fun toggleDailyWordsDay(day: DayOfWeek) {
        val days: Set<DayOfWeek> = toggleDailyWordsDayUseCase(day)
        setState { copy(dailyWordsDays = days) }
    }

    private fun selectDailyNewCardLimit(limit: DailyNewCardLimit) {
        dailyNewCardLimit.set(limit)
        productAnalytics.track(ProductEvent.DailyNewCardLimitSelected(limit.cards))
        setState { copy(dailyNewCardLimit = limit) }
    }

    private fun exportToUri(uri: Uri) {
        viewModelScope.launch {
            setState { copy(isExporting = true) }
            exportDataSource.export(uri)
                .onSuccess {
                    sendEffect(SettingsUiEffect.ShowSuccess(R.string.settings_backup_exported))
                }
                .onFailure { error ->
                    logError(TAG, "export:error ${error.message}", error)
                    sendEffect(SettingsUiEffect.ShowError(R.string.settings_backup_export_failed))
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
                    sendEffect(SettingsUiEffect.ShowSuccess(R.string.settings_backup_restored))
                }
                .onFailure { error ->
                    logError(TAG, "import:error ${error.message}", error)
                    sendEffect(SettingsUiEffect.ShowError(importErrorMessage(error)))
                }
            setState { copy(isImporting = false, pendingImportUri = null) }
        }
    }

    private fun cancelImport() {
        setState { copy(isConfirmDialogVisible = false, pendingImportUri = null) }
    }

    @StringRes
    private fun importErrorMessage(error: Throwable): Int = when {
        error is IncompatibleSchemaException || hasCause<IncompatibleSchemaException>(error) ->
            R.string.settings_backup_incompatible
        else -> R.string.settings_backup_restore_failed
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

package com.emm.hello.newfeatures.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.R
import com.emm.hello.navigation.Navigator
import com.emm.hello.newfeatures.deck.DecksRoute
import com.emm.hello.notifications.requestPostNotificationsPermission
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
object SettingsRoute : NavKey

@Composable
fun SettingsDestination(navigator: Navigator) {
    val vm: SettingsViewModel = koinViewModel()
    val uiState: SettingsUiState by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context: Context = LocalContext.current
    val clipboard: Clipboard = LocalClipboard.current
    val copiedMessage: String = stringResource(R.string.settings_build_copied)
    val buildClipLabel: String = stringResource(R.string.settings_build_title)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        uri?.let { vm.onIntent(SettingsUiIntent.ExportUriReceived(it)) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { vm.onIntent(SettingsUiIntent.ImportUriReceived(it)) }
    }

    val permissionLauncher: ManagedActivityResultLauncher<String, Boolean> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { vm.onIntent(SettingsUiIntent.NotificationPermissionSettled) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.onIntent(SettingsUiIntent.RefreshNotificationPermission)
    }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is SettingsUiEffect.ShowSuccess -> {
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
                }
                is SettingsUiEffect.ShowError -> {
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
                }
                SettingsUiEffect.LaunchExportPicker -> {
                    exportLauncher.launch("hello-backup-${System.currentTimeMillis()}.json")
                }
                SettingsUiEffect.LaunchImportPicker -> {
                    importLauncher.launch(arrayOf("application/json"))
                }
                SettingsUiEffect.RequestNotificationPermission -> {
                    permissionLauncher.requestPostNotificationsPermission {
                        vm.onIntent(SettingsUiIntent.NotificationPermissionSettled)
                    }
                }
                SettingsUiEffect.OpenNotificationSettings -> {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }
                is SettingsUiEffect.CopyToClipboard -> {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(buildClipLabel, effect.text)))
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                    }
                }
            }
        }
    }

    SettingsScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onExport = { vm.onIntent(SettingsUiIntent.ExportData) },
        onImport = { vm.onIntent(SettingsUiIntent.ImportData) },
        onConfirmImport = { vm.onIntent(SettingsUiIntent.ConfirmImport) },
        onDismissImport = { vm.onIntent(SettingsUiIntent.CancelImport) },
        onNavigateBack = { navigator.goBack() },
        onDecks = { navigator.navigateTo(DecksRoute) },
        onReminderEnabledChange = { vm.onIntent(SettingsUiIntent.SetReminderEnabled(it)) },
        onEditReminderTime = { vm.onIntent(SettingsUiIntent.EditReminderTime) },
        onReminderTimeChange = { vm.onIntent(SettingsUiIntent.SetReminderTime(it)) },
        onDismissReminderTimePicker = { vm.onIntent(SettingsUiIntent.DismissReminderTimePicker) },
        onOpenNotificationSettings = { vm.onIntent(SettingsUiIntent.OpenNotificationSettings) },
        onLinkGoogleAccount = { vm.onIntent(SettingsUiIntent.LinkGoogleAccount) },
        onCopyBuildInfo = { vm.onIntent(SettingsUiIntent.CopyBuildInfo) },
    )
}

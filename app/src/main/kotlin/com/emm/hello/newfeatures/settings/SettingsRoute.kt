package com.emm.hello.newfeatures.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
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

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        uri?.let { vm.onExportUri(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { vm.onImportUri(it) }
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
            }
        }
    }

    SettingsScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onExport = { exportLauncher.launch("hello-backup-${System.currentTimeMillis()}.json") },
        onImport = { importLauncher.launch(arrayOf("application/json")) },
        onConfirmImport = { vm.onIntent(SettingsUiIntent.ConfirmImport) },
        onDismissImport = { vm.onIntent(SettingsUiIntent.CancelImport) },
        onNavigateBack = { navigator.goBack() },
    )
}

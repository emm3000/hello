package com.emm.hello.newfeatures.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
object SettingsRoute

fun NavGraphBuilder.settings(navController: NavController) {
    composable<SettingsRoute> {
        val vm: SettingsViewModel = koinViewModel()
        val uiState: SettingsUiState by vm.uiState.collectAsStateWithLifecycle()

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
                    is SettingsUiEffect.ShowSuccess -> { /* snackbar handled by screen */ }
                    is SettingsUiEffect.ShowError -> { /* snackbar handled by screen */ }
                }
            }
        }

        SettingsScreen(
            state = uiState,
            onExport = { exportLauncher.launch("hello-backup-${System.currentTimeMillis()}.json") },
            onImport = { importLauncher.launch(arrayOf("application/json")) },
            onConfirmImport = { vm.onIntent(SettingsUiIntent.ConfirmImport) },
            onDismissImport = { vm.onIntent(SettingsUiIntent.CancelImport) },
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

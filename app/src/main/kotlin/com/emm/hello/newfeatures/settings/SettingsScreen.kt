package com.emm.hello.newfeatures.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emm.hello.R
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.CardVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HCard
import com.emm.hello.core.ui.HCardContent
import com.emm.hello.core.ui.HCardHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    onConfirmImport: () -> Unit = {},
    onDismissImport: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Export Section
            HCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.Elevated,
            ) {
                HCardHeader(
                    title = stringResource(R.string.backup_section_title),
                    description = stringResource(R.string.backup_section_description),
                )
                HCardContent {
                    HAlert(
                        title = stringResource(R.string.backup_warning_title),
                        description = stringResource(R.string.backup_warning_description),
                        variant = AlertVariant.Warning,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    HButton(
                        text = stringResource(R.string.export_data),
                        onClick = onExport,
                        variant = ButtonVariant.Default,
                        isLoading = state.isExporting,
                        leadingIcon = Icons.Default.Download,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HButton(
                        text = stringResource(R.string.restore_backup),
                        onClick = onImport,
                        variant = ButtonVariant.Outline,
                        isLoading = state.isImporting,
                        leadingIcon = Icons.Default.Upload,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }

    // Confirmation dialog for import
    if (state.isConfirmDialogVisible) {
        HAlertDialog(
            onDismiss = onDismissImport,
            onConfirm = onConfirmImport,
            title = stringResource(R.string.restore_confirm_title),
            description = stringResource(R.string.restore_confirm_description),
            confirmText = stringResource(R.string.restore_confirm_yes),
            cancelText = stringResource(R.string.restore_confirm_no),
            isDangerous = true,
        )
    }
}

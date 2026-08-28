package com.emm.hello.newfeatures.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.destructiveInk
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.surface
import com.emm.hello.core.theme.schibsted
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HLoadingSpinner
import com.emm.hello.core.ui.HSectionLabel
import com.emm.hello.core.ui.HSeparator
import com.emm.hello.core.ui.HTopBar

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
    onDecks: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = pageBackground,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                HTopBar(onBack = onNavigateBack)

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 40.dp,
                    ),
                ) {
                    item {
                        SettingsHeader()
                        Spacer(Modifier.height(32.dp))
                    }
                    item {
                        OrganizationSection(onDecks = onDecks)
                        Spacer(Modifier.height(28.dp))
                    }
                    item {
                        DataSection(
                            isExporting = state.isExporting,
                            isImporting = state.isImporting,
                            onExport = onExport,
                            onImport = onImport,
                        )
                        Spacer(Modifier.height(40.dp))
                    }
                    item { SettingsFooter() }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp),
            )
        }
    }

    if (state.isConfirmDialogVisible) {
        HAlertDialog(
            onDismiss = onDismissImport,
            onConfirm = onConfirmImport,
            title = stringResource(R.string.restore_confirm_title),
            description = stringResource(R.string.restore_confirm_description),
            confirmText = stringResource(R.string.restore_confirm_yes),
            cancelText = stringResource(R.string.restore_confirm_no),
            icon = Icons.Outlined.Delete,
            isDangerous = true,
        )
    }
}

@Composable
private fun SettingsHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.settings_eyebrow).uppercase(),
            fontFamily = schibsted,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.12.em,
            color = inkMuted,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.settings_headline),
            fontWeight = FontWeight.SemiBold,
            fontSize = 44.sp,
            lineHeight = (44 * 1.04f).sp,
            letterSpacing = (-0.02).em,
            color = ink,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.settings_subtitle),
            fontSize = 18.sp,
            color = inkMuted,
        )
    }
}

@Composable
private fun OrganizationSection(onDecks: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HSectionLabel(label = stringResource(R.string.settings_section_organization))
        Spacer(Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surface,
            shape = RoundedCornerShape(16.dp),
        ) {
            SettingsRow(
                icon = Icons.AutoMirrored.Outlined.List,
                title = stringResource(R.string.decks_title),
                sub = stringResource(R.string.settings_decks_subtitle),
                onClick = onDecks,
            )
        }
    }
}

@Composable
private fun DataSection(
    isExporting: Boolean,
    isImporting: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HSectionLabel(label = stringResource(R.string.settings_section_data))
        Spacer(Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surface,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Outlined.Download,
                    title = stringResource(R.string.export_data),
                    sub = stringResource(R.string.settings_export_subtitle),
                    isBusy = isExporting,
                    onClick = onExport,
                )
                HSeparator(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.Upload,
                    title = stringResource(R.string.restore_backup),
                    sub = stringResource(R.string.settings_import_subtitle),
                    subTone = SubTone.Danger,
                    isBusy = isImporting,
                    onClick = onImport,
                )
            }
        }
    }
}

private enum class SubTone { Muted, Danger }

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    sub: String? = null,
    subTone: SubTone = SubTone.Muted,
    isBusy: Boolean = false,
    onClick: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = { ChevronTrailing() },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isBusy) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(22.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = schibsted,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = ink,
            )
            if (sub != null) {
                val subColor = when (subTone) {
                    SubTone.Muted -> inkMuted
                    SubTone.Danger -> destructiveInk
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = sub,
                    fontFamily = schibsted,
                    fontSize = 13.sp,
                    color = subColor,
                    lineHeight = 18.sp,
                )
            }
        }

        if (isBusy) {
            HLoadingSpinner(
                size = 18.dp,
                color = ink,
                strokeWidth = 2.dp,
            )
        } else {
            trailing()
        }
    }
}

@Composable
private fun RowScope.ChevronTrailing() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = inkMuted,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun SettingsFooter() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_footer_tagline),
            fontSize = 16.sp,
            color = inkMuted,
        )
        Text(
            text = stringResource(R.string.settings_footer_meta),
            fontFamily = schibsted,
            fontSize = 11.sp,
            letterSpacing = 0.12.em,
            color = Color(red = 244, green = 239, blue = 230, alpha = 90),
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsScreenPreview() {
    HelloTheme {
        SettingsScreen(
            state = SettingsUiState(),
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun SettingsScreenBusyPreview() {
    HelloTheme {
        SettingsScreen(
            state = SettingsUiState(isImporting = true),
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

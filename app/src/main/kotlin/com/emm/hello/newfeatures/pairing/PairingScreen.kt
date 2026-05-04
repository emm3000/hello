package com.emm.hello.newfeatures.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emm.domain.sync.LinkedDevice
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HButton

@Composable
fun PairingScreen(
    state: PairingUiState,
    onBack: () -> Unit,
    onIntent: (PairingUiIntent) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Vincular Dispositivos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.remoteAvailable) {
                item {
                    HAlert(
                        title = "Modo local-only activo",
                        description = "La vinculación remota está pausada temporalmente. Podés seguir usando la app y guardando cambios locales.",
                        variant = AlertVariant.Warning,
                    )
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Crear código para nuevo dispositivo", fontWeight = FontWeight.SemiBold)
                        HButton(
                            text = "Crear código (6 dígitos)",
                            onClick = { onIntent(PairingUiIntent.CreateCodeClicked) },
                            enabled = state.remoteAvailable,
                            isLoading = state.isGeneratingCode,
                        )
                        state.generatedCode?.let { code ->
                            Text("Código: $code", style = MaterialTheme.typography.headlineSmall)
                        }
                        state.generatedCodeExpiresAt?.let { expires ->
                            Text("Expira: $expires", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Unirse con código", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = state.joinCode,
                            onValueChange = { onIntent(PairingUiIntent.JoinCodeChanged(it)) },
                            label = { Text("Código de 6 dígitos") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.remoteAvailable,
                            singleLine = true,
                        )
                        HButton(
                            text = "Vincular dispositivo",
                            onClick = { onIntent(PairingUiIntent.JoinWithCodeClicked) },
                            enabled = state.remoteAvailable,
                            isLoading = state.isSubmittingJoin,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Dispositivos vinculados", fontWeight = FontWeight.SemiBold)
                    HButton(
                        text = "Refrescar",
                        onClick = { onIntent(PairingUiIntent.RefreshDevicesClicked) },
                        variant = ButtonVariant.Ghost,
                        enabled = state.remoteAvailable,
                        isLoading = state.isLoading,
                    )
                }
            }

            items(state.devices, key = { it.id }) { device ->
                DeviceRow(
                    device = device,
                    remoteAvailable = state.remoteAvailable,
                    onRevoke = { onIntent(PairingUiIntent.RevokeDeviceClicked(it)) },
                )
            }

            item {
                HButton(
                    text = "Volver",
                    onClick = onBack,
                    variant = ButtonVariant.Outline,
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: LinkedDevice,
    remoteAvailable: Boolean,
    onRevoke: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = device.deviceName ?: device.id, fontWeight = FontWeight.Medium)
            Text(
                text = "Platform: ${device.platform ?: "unknown"}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = if (device.revokedAt == null) "Estado: activo" else "Estado: revocado",
                style = MaterialTheme.typography.bodySmall,
            )
            if (!device.isCurrent && device.revokedAt == null) {
                HButton(
                    text = "Revocar",
                    onClick = { onRevoke(device.id) },
                    variant = ButtonVariant.Destructive,
                    enabled = remoteAvailable,
                )
            }
            if (device.isCurrent) {
                Text("Este dispositivo", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

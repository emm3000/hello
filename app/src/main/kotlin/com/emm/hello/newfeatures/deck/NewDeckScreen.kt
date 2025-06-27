package com.emm.hello.newfeatures.deck

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDeckScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    state: NewDeckUiState = NewDeckUiState(),
    onAction: (NewDeckAction) -> Unit = {},
) {

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nuevo Mazo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "Crea un nuevo mazo",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Dale un nombre y una descripción para organizar tus tarjetas.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(48.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = { onAction(NewDeckAction.NameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre del mazo") },
                placeholder = { Text("Ej: Vocabulario de Inglés B2") },
                leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.description,
                onValueChange = { onAction(NewDeckAction.DescriptionChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("Descripción (opcional)") },
                placeholder = { Text("Un mazo para los verbos irregulares más comunes.") },
                leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onAction(NewDeckAction.Submit)
                    onNavigateBack()
                },
                enabled = state.name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(50.dp)
            ) {
                Icon(
                    Icons.Filled.Save,
                    contentDescription = "Guardar",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Guardar Mazo")
            }
        }
    }
}

@PreviewLightDark
@Composable
fun NewDeckScreenPreview() {
    HelloTheme {
        NewDeckScreen()
    }
}
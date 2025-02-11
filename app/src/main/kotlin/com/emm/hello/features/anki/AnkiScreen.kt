package com.emm.hello.features.anki

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme

@Composable
fun AnkiScreen(
    state: AnkiUiState,
    onGenerate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {

    val input = remember {
        mutableStateOf("")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(15.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = input.value,
            onValueChange = { input.value = it },
            placeholder = {
                Text(
                    text = "Enter a word",
                    color = MaterialTheme.colorScheme.onBackground.copy(0.5f)
                )
            },
            textStyle = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
            ),
            trailingIcon = {
                IconButton(
                    onClick = { onGenerate(input.value) },
                    enabled = input.value.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Send,
                        contentDescription = null,
                    )
                }
            }
        )

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        if (state.anki != null) {
            Text(state.anki.pos)
        }
    }
}

@PreviewLightDark
@Composable
private fun AnkiScreenPreview() {
    HelloTheme {
        Surface {
            AnkiScreen(AnkiUiState(), {})
        }
    }
}
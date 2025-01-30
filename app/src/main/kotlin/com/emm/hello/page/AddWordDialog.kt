package com.emm.hello.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.ui.theme.HelloTheme

@Composable
fun AddWordDialog(
    onDismiss: () -> Unit,
    onAddWord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Add word",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val (word, setWord) = remember {
            mutableStateOf("")
        }
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = word,
            onValueChange = setWord,
            placeholder = {
                Text(
                    text = "insert new word",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            },
            label = {
                Text(
                    text = "Word",
                )
            }
        )
        val keyboardController = LocalSoftwareKeyboardController.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    onAddWord(word)
                }
            ) {
                Text(
                    text = "Agregar",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            TextButton(
                onClick = {
                    onDismiss()
                }
            ) {
                Text(
                    text = "Cancelar",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AddWordDialogPreview() {
    HelloTheme {
        AddWordDialog(
            onDismiss = {},
            onAddWord = {}
        )
    }
}
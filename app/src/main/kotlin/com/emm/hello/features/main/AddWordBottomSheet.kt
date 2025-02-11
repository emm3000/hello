package com.emm.hello.features.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordBottomSheet(
    showDialog: MutableState<Boolean>,
    wordCreate: (String) -> Unit,
) {
    ModalBottomSheet(
        modifier = Modifier.imePadding(),
        onDismissRequest = { showDialog.value = false },
        dragHandle = null,
    ) {
        ModalBottomContent(wordCreate)
    }
}

@Composable
private fun ModalBottomContent(
    wordCreate: (String) -> Unit = {}
) {
    val request = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Add word",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
        )

        val inputText = remember {
            mutableStateOf("")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = inputText.value,
                onValueChange = { inputText.value = it },
                placeholder = {
                    Text("Write here . . .")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(request)
                    .weight(1f),
                singleLine = true,
                keyboardActions = KeyboardActions {
                    wordCreate(inputText.value)
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false,
                )
            )

            OutlinedIconButton(
                onClick = {
                    wordCreate(inputText.value)
                },
                enabled = inputText.value.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Publish,
                    contentDescription = null,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomSheetPreview() {
    HelloTheme {
        ModalBottomContent {}
    }
}
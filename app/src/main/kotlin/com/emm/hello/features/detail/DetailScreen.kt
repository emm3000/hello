package com.emm.hello.features.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

@Composable
fun DetailScreen(
    wordName: String,
    updateWord: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val (isEditable, setIsEditable) = remember {
            mutableStateOf(false)
        }

        val (wordField, setWordField) = remember {
            mutableStateOf(TextFieldValue(wordName))
        }

        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(isEditable) {
            if (isEditable) {
                focusRequester.requestFocus()
                setWordField(
                    wordField.copy(selection = TextRange(wordField.text.length))
                )
            }
        }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            value = wordField,
            onValueChange = setWordField,
            enabled = isEditable,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                disabledBorderColor = MaterialTheme.colorScheme.onBackground
            ),
            trailingIcon = {
                if (isEditable) {
                    EditableIconsButtons(setWordField, updateWord, wordField, setIsEditable)
                } else {
                    NonEditableIconButton(setIsEditable)
                }
            },
        )
    }
}

@Composable
private fun EditableIconsButtons(
    setWordField: (TextFieldValue) -> Unit,
    updateWord: (String) -> Unit,
    wordField: TextFieldValue,
    setIsEditable: (Boolean) -> Unit
) {
    Row {
        IconButton(onClick = { setWordField(TextFieldValue()) }) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(
            onClick = {
                updateWord(wordField.text)
                setIsEditable(false)
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun NonEditableIconButton(
    setIsEditable: (Boolean) -> Unit,
) {
    IconButton(onClick = {
        setIsEditable(true)
    }) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@PreviewLightDark
@Composable
private fun DetailScreenPreview() {
    HelloTheme {
        DetailScreen(
            wordName = "random word",
            updateWord = { }
        )
    }
}
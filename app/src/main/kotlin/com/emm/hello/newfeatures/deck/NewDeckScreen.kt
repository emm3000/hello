package com.emm.hello.newfeatures.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HTagInput

@Composable
fun NewDeckScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    state: NewDeckUiState = NewDeckUiState(),
    onIntent: (NewDeckUiIntent) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val isEditMode = state.formMode is DeckFormMode.Edit
    val title = if (isEditMode) stringResource(R.string.edit_deck_title) else stringResource(R.string.new_deck_title)
    val saveText = if (isEditMode) stringResource(R.string.save_changes) else stringResource(R.string.save)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    HButton(
                        text = saveText,
                        onClick = {
                            focusManager.clearFocus()
                            onIntent(NewDeckUiIntent.Submit)
                        },
                        enabled = state.isValid && !state.isLoading,
                        isLoading = state.isLoading,
                        variant = ButtonVariant.Ghost,
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HInput(
                value = state.name,
                onValueChange = { onIntent(NewDeckUiIntent.NameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.deck_name_label),
                placeholder = stringResource(R.string.deck_name_placeholder),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
            )

            HInput(
                value = state.description,
                onValueChange = { onIntent(NewDeckUiIntent.DescriptionChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.deck_description_label),
                placeholder = stringResource(R.string.deck_description_placeholder),
                supportingText = stringResource(R.string.optional),
                singleLine = false,
                minLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
            )

            HTagInput(
                tags = state.tags,
                onTagsChange = { onIntent(NewDeckUiIntent.TagsChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun NewDeckScreenPreview() {
    HelloTheme {
        NewDeckScreen()
    }
}

package com.emm.hello.newfeatures.deck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HIconButton
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
    val topLabel = if (isEditMode) {
        stringResource(R.string.edit_deck_top_label)
    } else {
        stringResource(R.string.new_deck_top_label)
    }
    val headline = if (isEditMode) {
        stringResource(R.string.edit_deck_headline)
    } else {
        stringResource(R.string.new_deck_headline)
    }
    val actionLabel = if (isEditMode) {
        stringResource(R.string.new_deck_action_save)
    } else {
        stringResource(R.string.new_deck_action_create)
    }
    val nameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.formMode) {
        if (!isEditMode) {
            nameFocusRequester.requestFocus()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = pageBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding(),
        ) {
            NewDeckTopBar(
                title = topLabel,
                actionLabel = actionLabel,
                actionEnabled = state.isValid && !state.isLoading,
                onClose = onNavigateBack,
                onAction = {
                    focusManager.clearFocus()
                    onIntent(NewDeckUiIntent.Submit)
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.screenGutter)
                    .padding(top = 6.dp, bottom = 32.dp),
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.displayMedium,
                    color = ink,
                )

                Spacer(Modifier.height(28.dp))

                HInput(
                    value = state.name,
                    onValueChange = { onIntent(NewDeckUiIntent.NameChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocusRequester),
                    label = stringResource(R.string.deck_name_label),
                    placeholder = stringResource(R.string.deck_name_placeholder),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                )

                Spacer(Modifier.height(MaterialTheme.spacing.lg))

                HInput(
                    value = state.description,
                    onValueChange = { onIntent(NewDeckUiIntent.DescriptionChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.deck_description_label),
                    placeholder = stringResource(R.string.deck_description_placeholder),
                    supportingText = stringResource(R.string.optional),
                    singleLine = false,
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                )

                Spacer(Modifier.height(MaterialTheme.spacing.lg))

                HTagInput(
                    tags = state.tags,
                    onTagsChange = { onIntent(NewDeckUiIntent.TagsChanged(it)) },
                    label = stringResource(R.string.tags_label),
                    supportingText = stringResource(R.string.tags_supporting_text),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.canDelete) {
                    Spacer(Modifier.height(40.dp))
                    HButton(
                        text = stringResource(R.string.delete_deck_action),
                        onClick = { onIntent(NewDeckUiIntent.DeleteDeck) },
                        variant = HButtonVariant.Text,
                        danger = true,
                        full = true,
                    )
                }
            }
        }
    }

    if (state.isDeleteConfirmationVisible) {
        HAlertDialog(
            title = stringResource(R.string.delete_deck_title),
            description = stringResource(R.string.delete_deck_description),
            icon = Icons.Outlined.Delete,
            confirmText = stringResource(R.string.delete),
            cancelText = stringResource(R.string.cancel),
            isDangerous = true,
            onConfirm = { onIntent(NewDeckUiIntent.ConfirmDeleteDeck) },
            onDismiss = { onIntent(NewDeckUiIntent.DismissDeleteDeck) },
        )
    }
}

@Composable
private fun NewDeckTopBar(
    title: String,
    actionLabel: String,
    actionEnabled: Boolean,
    onClose: () -> Unit,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HIconButton(
            icon = Icons.Default.Close,
            contentDescription = stringResource(R.string.new_deck_close_content_description),
            onClick = onClose,
            buttonSize = 44.dp,
        )

        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.metadata,
            color = inkMuted,
            modifier = Modifier.weight(1f),
        )

        val actionColor = if (actionEnabled) ink else inkMuted
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.titleSmall,
            color = actionColor,
            modifier = Modifier
                .padding(end = 6.dp)
                .then(if (actionEnabled) Modifier.clickable(onClick = onAction) else Modifier)
                .padding(horizontal = 10.dp, vertical = 10.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun NewDeckCreatePreview() {
    HelloTheme {
        NewDeckScreen(
            state = NewDeckUiState(
                name = "",
                description = "",
                tags = emptyList(),
                formMode = DeckFormMode.Create,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun NewDeckEditPreview() {
    HelloTheme {
        NewDeckScreen(
            state = NewDeckUiState(
                name = "Vocabulario editorial",
                description = "Palabras curadas para probar el detalle.",
                tags = listOf("inglés", "editorial"),
                formMode = DeckFormMode.Edit("deck-1"),
            ),
        )
    }
}

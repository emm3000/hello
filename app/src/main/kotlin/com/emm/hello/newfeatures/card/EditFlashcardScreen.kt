package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.cardHueFor
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.schibsted
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HFieldVariant
import com.emm.hello.core.ui.HIconButton
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HLoadingSpinner

@Composable
fun EditFlashcardScreen(
    state: EditFlashcardUiState,
    onIntent: (EditFlashcardUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = cardHueFor(state.flashcardId),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = MaterialTheme.spacing.screenGutter)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            EditTopBar(state = state, onIntent = onIntent)

            if (state.isLoading) {
                LoadingBody(modifier = Modifier.weight(1f))
            } else {
                FieldList(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (state.isDeleteConfirmationVisible) {
        HAlertDialog(
            title = stringResource(R.string.delete_flashcard_title),
            description = stringResource(R.string.delete_flashcard_description),
            confirmText = stringResource(R.string.delete),
            cancelText = stringResource(R.string.cancel),
            isDangerous = true,
            onConfirm = { onIntent(EditFlashcardUiIntent.ConfirmDeleteFlashcard) },
            onDismiss = { onIntent(EditFlashcardUiIntent.DismissDeleteFlashcard) },
        )
    }
}

@Composable
private fun EditTopBar(
    state: EditFlashcardUiState,
    onIntent: (EditFlashcardUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HIconButton(
            icon = Icons.Default.Close,
            contentDescription = stringResource(R.string.edit_flashcard_close_content_description),
            onClick = { onIntent(EditFlashcardUiIntent.CloseClicked) },
            buttonSize = 44.dp,
        )

        Spacer(modifier = Modifier.weight(1f))

        HButton(
            text = stringResource(R.string.edit_flashcard_action_save),
            onClick = { onIntent(EditFlashcardUiIntent.Submit) },
            variant = HButtonVariant.Text,
            enabled = state.isValid && !state.isSubmitting && !state.isLoading,
        )
    }
}

@Composable
private fun FieldList(
    state: EditFlashcardUiState,
    onIntent: (EditFlashcardUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.edit_flashcard_title).uppercase(),
            fontFamily = schibsted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.12.em,
            color = inkMuted,
        )

        HInput(
            value = state.word,
            onValueChange = { onIntent(EditFlashcardUiIntent.WordChanged(it)) },
            label = stringResource(R.string.word_label),
            placeholder = stringResource(R.string.edit_flashcard_word_placeholder),
            errorMessage = state.wordError?.let { stringResource(it) },
            variant = HFieldVariant.Underline,
        )

        HInput(
            value = state.translation,
            onValueChange = { onIntent(EditFlashcardUiIntent.TranslationChanged(it)) },
            label = stringResource(R.string.translation_label),
            placeholder = stringResource(R.string.edit_flashcard_translation_placeholder),
            variant = HFieldVariant.Underline,
        )

        HInput(
            value = state.exampleText,
            onValueChange = { onIntent(EditFlashcardUiIntent.ExampleTextChanged(it)) },
            label = stringResource(R.string.example_sentence_label),
            variant = HFieldVariant.Underline,
            singleLine = false,
            minLines = 2,
        )

        HInput(
            value = state.exampleTranslation,
            onValueChange = { onIntent(EditFlashcardUiIntent.ExampleTranslationChanged(it)) },
            label = stringResource(R.string.example_translation_label),
            variant = HFieldVariant.Underline,
            singleLine = false,
            minLines = 2,
        )

        HInput(
            value = state.partOfSpeech,
            onValueChange = { onIntent(EditFlashcardUiIntent.PartOfSpeechChanged(it)) },
            label = stringResource(R.string.part_of_speech_label),
            placeholder = stringResource(R.string.edit_flashcard_part_of_speech_placeholder),
            variant = HFieldVariant.Underline,
        )

        HInput(
            value = state.phonetic,
            onValueChange = { onIntent(EditFlashcardUiIntent.PhoneticChanged(it)) },
            label = stringResource(R.string.phonetics_label),
            placeholder = stringResource(R.string.edit_flashcard_phonetic_placeholder),
            variant = HFieldVariant.Underline,
        )

        Spacer(modifier = Modifier.heightIn(min = 8.dp))

        HButton(
            text = stringResource(R.string.edit_flashcard_delete_action),
            onClick = { onIntent(EditFlashcardUiIntent.DeleteFlashcard) },
            variant = HButtonVariant.Text,
            danger = true,
        )
    }
}

@Composable
private fun LoadingBody(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        HLoadingSpinner(color = ink)
    }
}

@PreviewLightDark
@Composable
private fun EditFlashcardScreenPreview() {
    HelloTheme {
        EditFlashcardScreen(
            state = EditFlashcardUiState(
                flashcardId = "card-1",
                isLoading = false,
                word = "aesthetic",
                translation = "estético",
                exampleText = "The building has a strong aesthetic.",
                exampleTranslation = "El edificio tiene una estética marcada.",
                partOfSpeech = "adjective",
                phonetic = "/esˈθetɪk/",
            ),
            onIntent = {},
        )
    }
}

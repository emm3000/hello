package com.emm.hello.newfeatures.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.domain.flashcard.Example
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberBad
import com.emm.hello.core.theme.emberBadSoft
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberDivider
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberOnBg
import com.emm.hello.core.theme.emberPrimary
import com.emm.hello.core.theme.emberSurface
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.instrumentSerif
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HSectionLabel

@Composable
fun EditFlashcardScreen(
    modifier: Modifier = Modifier,
    state: EditFlashcardUiState = EditFlashcardUiState(),
    onNavigateBack: () -> Unit = {},
    onIntent: (EditFlashcardUiIntent) -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = emberBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding(),
        ) {
            EditFlashcardTopBar(
                actionEnabled = state.isValid && !state.isSubmitting && !state.isLoading,
                onClose = onNavigateBack,
                onSave = { onIntent(EditFlashcardUiIntent.Submit) },
            )

            if (state.isLoading) {
                LoadingBody()
            } else {
                EditFlashcardBody(state = state, onIntent = onIntent)
            }
        }
    }

    if (state.isDeleteConfirmationVisible) {
        HAlertDialog(
            title = stringResource(R.string.delete_flashcard_title),
            description = stringResource(R.string.delete_flashcard_description),
            icon = Icons.Outlined.Delete,
            confirmText = stringResource(R.string.delete),
            cancelText = stringResource(R.string.cancel),
            isDangerous = true,
            onConfirm = { onIntent(EditFlashcardUiIntent.ConfirmDeleteFlashcard) },
            onDismiss = { onIntent(EditFlashcardUiIntent.DismissDeleteFlashcard) },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar — close · mono title · accent Guardar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditFlashcardTopBar(
    actionEnabled: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    val title = stringResource(R.string.edit_flashcard_title).uppercase()
    val actionLabel = stringResource(R.string.edit_flashcard_action_save)
    val actionColor = if (actionEnabled) emberAccent else emberMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.edit_flashcard_close_content_description),
                tint = emberPrimary,
            )
        }

        Text(
            text = title,
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.12.em,
            color = emberMuted,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = actionLabel,
            fontFamily = geist,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = actionColor,
            modifier = Modifier
                .padding(end = 6.dp)
                .then(if (actionEnabled) Modifier.clickable(onClick = onSave) else Modifier)
                .padding(horizontal = 10.dp, vertical = 10.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading body
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingBody() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.edit_flashcard_loading),
            fontFamily = geist,
            fontSize = 14.sp,
            color = emberMuted,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Form body
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditFlashcardBody(
    state: EditFlashcardUiState,
    onIntent: (EditFlashcardUiIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp, bottom = 32.dp),
    ) {
        HInput(
            value = state.word,
            onValueChange = { onIntent(EditFlashcardUiIntent.WordChanged(it)) },
            label = stringResource(R.string.word_label),
            placeholder = stringResource(R.string.edit_flashcard_word_placeholder),
            errorMessage = state.wordError?.let { stringResource(it) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))

        HInput(
            value = state.translation,
            onValueChange = { onIntent(EditFlashcardUiIntent.TranslationChanged(it)) },
            label = stringResource(R.string.translation_label),
            placeholder = stringResource(R.string.edit_flashcard_translation_placeholder),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HInput(
                value = state.phonetic,
                onValueChange = { onIntent(EditFlashcardUiIntent.PhoneticChanged(it)) },
                label = stringResource(R.string.phonetics_label),
                placeholder = stringResource(R.string.edit_flashcard_phonetic_placeholder),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            HInput(
                value = state.partOfSpeech,
                onValueChange = { onIntent(EditFlashcardUiIntent.PartOfSpeechChanged(it)) },
                label = stringResource(R.string.part_of_speech_label),
                placeholder = stringResource(R.string.edit_flashcard_part_of_speech_placeholder),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(18.dp))

        HInput(
            value = state.meaning,
            onValueChange = { onIntent(EditFlashcardUiIntent.MeaningChanged(it)) },
            label = stringResource(R.string.meaning_label),
            placeholder = stringResource(R.string.edit_flashcard_meaning_placeholder),
            errorMessage = state.meaningError?.let { stringResource(it) },
            singleLine = false,
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))

        ExamplesSection(
            examples = state.examples,
            onIntent = onIntent,
        )

        Spacer(Modifier.height(40.dp))

        DangerRow(onDeleteClick = { onIntent(EditFlashcardUiIntent.DeleteFlashcard) })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Examples — section label + inline "+ agregar" + ExampleEditRow cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExamplesSection(
    examples: List<Example>,
    onIntent: (EditFlashcardUiIntent) -> Unit,
) {
    val baseLabel = stringResource(R.string.examples_label)
    val sectionLabel = "$baseLabel · ${examples.size}"

    HSectionLabel(
        label = sectionLabel,
        action = {
            Text(
                text = stringResource(R.string.edit_flashcard_add_inline),
                fontFamily = geist,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = emberAccent,
                modifier = Modifier
                    .clickable { onIntent(EditFlashcardUiIntent.AddExample) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        },
    )

    Spacer(Modifier.height(10.dp))

    if (examples.isEmpty()) {
        Text(
            text = stringResource(R.string.edit_flashcard_no_examples),
            fontFamily = instrumentSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
            color = emberMuted,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            examples.forEachIndexed { index, example ->
                ExampleEditRow(
                    example = example,
                    onTextChange = { onIntent(EditFlashcardUiIntent.ExampleTextChanged(index, it)) },
                    onTranslationChange = {
                        onIntent(EditFlashcardUiIntent.ExampleTranslationChanged(index, it))
                    },
                    onRemove = { onIntent(EditFlashcardUiIntent.RemoveExample(index)) },
                )
            }
        }
    }
}

@Composable
private fun ExampleEditRow(
    example: Example,
    onTextChange: (String) -> Unit,
    onTranslationChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(emberSurface, RoundedCornerShape(14.dp))
            .border(1.dp, emberDivider, RoundedCornerShape(14.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 44.dp, top = 12.dp, bottom = 12.dp),
        ) {
            ItalicSerifField(
                value = example.text,
                onValueChange = onTextChange,
                placeholder = stringResource(R.string.example_sentence_label),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(emberDivider),
            )
            Spacer(Modifier.height(8.dp))
            GeistField(
                value = example.translation,
                onValueChange = onTranslationChange,
                placeholder = stringResource(R.string.example_translation_label),
            )
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.edit_flashcard_remove_example),
                tint = emberMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ItalicSerifField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontFamily = instrumentSerif,
                fontStyle = FontStyle.Italic,
                fontSize = 17.sp,
                color = emberMuted,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = false,
            maxLines = 3,
            textStyle = TextStyle(
                fontFamily = instrumentSerif,
                fontStyle = FontStyle.Italic,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                color = emberOnBg,
            ),
            cursorBrush = SolidColor(emberAccent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GeistField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontFamily = geist,
                fontSize = 14.sp,
                color = emberMuted,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = false,
            maxLines = 3,
            textStyle = TextStyle(
                fontFamily = geist,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = emberOnBg,
            ),
            cursorBrush = SolidColor(emberAccent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Danger row — "Borrar tarjeta"
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DangerRow(onDeleteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(emberBadSoft, RoundedCornerShape(14.dp))
            .border(1.dp, emberBad.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable { onDeleteClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = null,
            tint = emberBad,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.edit_flashcard_delete_action),
            fontFamily = geist,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = emberBad,
            modifier = Modifier.weight(1f),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun EditFlashcardScreenPreview() {
    HelloTheme {
        EditFlashcardScreen(
            state = EditFlashcardUiState(
                isLoading = false,
                word = "serendipity",
                meaning = "La suerte de encontrar algo bueno por casualidad",
                translation = "casualidad afortunada",
                phonetic = "/ˌserənˈdɪpɪti/",
                partOfSpeech = "noun",
                examples = listOf(
                    Example(
                        exampleId = "1",
                        text = "Finding that book was pure serendipity.",
                        translation = "Encontrar ese libro fue pura casualidad.",
                        type = "",
                    ),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun EditFlashcardLoadingPreview() {
    HelloTheme {
        EditFlashcardScreen(state = EditFlashcardUiState(isLoading = true))
    }
}

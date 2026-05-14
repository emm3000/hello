package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emm.domain.flashcard.Example
import com.emm.hello.R
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HSeparator
import com.emm.hello.core.theme.spacing

@Composable
fun EditFlashcardScreen(
    modifier: Modifier = Modifier,
    state: EditFlashcardUiState = EditFlashcardUiState(),
    onNavigateBack: () -> Unit = {},
    onIntent: (EditFlashcardUiIntent) -> Unit = {},
) {
    if (state.isLoading) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                EditFlashcardTopBar(onNavigateBack = onNavigateBack)
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.edit_flashcard_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            EditFlashcardTopBar(onNavigateBack = onNavigateBack)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            // ── Word ──────────────────────────────────────────────────
            HInput(
                value = state.word,
                onValueChange = { onIntent(EditFlashcardUiIntent.WordChanged(it)) },
                label = stringResource(R.string.word_label),
                placeholder = stringResource(R.string.edit_flashcard_word_placeholder),
                errorMessage = state.wordError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Meaning ───────────────────────────────────────────────
            HInput(
                value = state.meaning,
                onValueChange = { onIntent(EditFlashcardUiIntent.MeaningChanged(it)) },
                label = stringResource(R.string.meaning_label),
                placeholder = stringResource(R.string.edit_flashcard_meaning_placeholder),
                errorMessage = state.meaningError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Translation ──────────────────────────────────────────
            HInput(
                value = state.translation,
                onValueChange = { onIntent(EditFlashcardUiIntent.TranslationChanged(it)) },
                label = stringResource(R.string.translation_label),
                placeholder = stringResource(R.string.edit_flashcard_translation_placeholder),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Phonetic (optional) ──────────────────────────────────
            HInput(
                value = state.phonetic,
                onValueChange = { onIntent(EditFlashcardUiIntent.PhoneticChanged(it)) },
                label = stringResource(R.string.phonetics_label),
                placeholder = stringResource(R.string.edit_flashcard_phonetic_placeholder),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Part of speech (optional) ────────────────────────────
            HInput(
                value = state.partOfSpeech,
                onValueChange = { onIntent(EditFlashcardUiIntent.PartOfSpeechChanged(it)) },
                label = stringResource(R.string.part_of_speech_label),
                placeholder = stringResource(R.string.edit_flashcard_part_of_speech_placeholder),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            HSeparator()

            // ── Examples ──────────────────────────────────────────────
            ExamplesEditor(
                examples = state.examples,
                onIntent = onIntent,
            )

            Spacer(Modifier.height(MaterialTheme.spacing.md))

            // ── Submit ────────────────────────────────────────────────
            HButton(
                text = stringResource(R.string.save_changes),
                onClick = { onIntent(EditFlashcardUiIntent.Submit) },
                variant = ButtonVariant.Default,
                enabled = state.isValid && !state.isSubmitting,
                isLoading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(MaterialTheme.spacing.lg))
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun EditFlashcardTopBar(onNavigateBack: () -> Unit) {
    androidx.compose.material3.TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.edit_flashcard_title),
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
    )
}

// ── Examples editor ────────────────────────────────────────────────────────────

@Composable
private fun ExamplesEditor(
    examples: List<Example>,
    onIntent: (EditFlashcardUiIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        Text(
            text = stringResource(R.string.examples_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (examples.isEmpty()) {
            Text(
                text = stringResource(R.string.edit_flashcard_no_examples),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        examples.forEachIndexed { index, example ->
            ExampleItemEditor(
                index = index,
                example = example,
                isLast = index == examples.lastIndex,
                onTextChanged = { onIntent(EditFlashcardUiIntent.ExampleTextChanged(index, it)) },
                onTranslationChanged = { onIntent(EditFlashcardUiIntent.ExampleTranslationChanged(index, it)) },
                onRemove = { onIntent(EditFlashcardUiIntent.RemoveExample(index)) },
            )
        }

        HButton(
            text = stringResource(R.string.edit_flashcard_add_example),
            onClick = { onIntent(EditFlashcardUiIntent.AddExample) },
            variant = ButtonVariant.Outline,
            leadingIcon = Icons.Outlined.Add,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExampleItemEditor(
    index: Int,
    example: Example,
    isLast: Boolean,
    onTextChanged: (String) -> Unit,
    onTranslationChanged: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.edit_flashcard_example_number, index + 1),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.edit_flashcard_remove_example),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HInput(
            value = example.text,
            onValueChange = onTextChanged,
            label = stringResource(R.string.example_sentence_label),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        HInput(
            value = example.translation,
            onValueChange = onTranslationChanged,
            label = stringResource(R.string.example_translation_label),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!isLast) {
            HSeparator()
        }
    }
}
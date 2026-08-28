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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.time.SystemClock
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.bricolage
import com.emm.hello.core.theme.cardHues
import com.emm.hello.core.theme.destructiveInk
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.inkSoft
import com.emm.hello.core.theme.schibsted
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HDropdownMenu
import com.emm.hello.core.ui.HIconButton
import com.emm.hello.core.ui.HLoadingSpinner
import com.emm.hello.core.ui.HMenuItem
import com.emm.hello.core.ui.underlineFirstMatch
import kotlin.math.abs

@Composable
fun FlashcardDetailScreen(
    state: FlashcardDetailUiState,
    onIntent: (FlashcardDetailUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hue: Color = cardHues[abs(state.flashcard.id.value.hashCode()) % cardHues.size]

    Surface(
        modifier = modifier.fillMaxSize(),
        color = hue,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = MaterialTheme.spacing.screenGutter)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            DetailTopBar(onIntent = onIntent)

            if (state.isLoading) {
                LoadingBody(modifier = Modifier.weight(1f))
            } else {
                CardBody(
                    flashcard = state.flashcard,
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
            onConfirm = { onIntent(FlashcardDetailUiIntent.ConfirmDeleteFlashcard) },
            onDismiss = { onIntent(FlashcardDetailUiIntent.DismissDeleteFlashcard) },
        )
    }
}

@Composable
private fun DetailTopBar(
    onIntent: (FlashcardDetailUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuExpanded: Boolean by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            onClick = { onIntent(FlashcardDetailUiIntent.BackClicked) },
            buttonSize = 44.dp,
        )

        Spacer(modifier = Modifier.weight(1f))

        HButton(
            text = stringResource(R.string.edit),
            onClick = { onIntent(FlashcardDetailUiIntent.EditFlashcard) },
            variant = HButtonVariant.Text,
        )

        Box {
            HIconButton(
                icon = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more_options),
                onClick = { isMenuExpanded = true },
                buttonSize = 44.dp,
            )
            HDropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                items = listOf(
                    HMenuItem(
                        label = stringResource(R.string.delete),
                        onClick = {
                            isMenuExpanded = false
                            onIntent(FlashcardDetailUiIntent.DeleteFlashcard)
                        },
                        isDestructive = true,
                    ),
                ),
            )
        }
    }
}

@Composable
private fun CardBody(
    flashcard: Flashcard,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        WordBlock(flashcard = flashcard)

        if (flashcard.translation.isNotBlank()) {
            Text(
                text = flashcard.translation,
                fontFamily = bricolage,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.02).em,
                color = ink,
            )
        }

        ExampleBlock(example = flashcard.examples.firstOrNull(), word = flashcard.word)

        ReferenceLine(flashcard = flashcard)

        StatusLine(status = flashcard.enrichmentStatus)
    }
}

@Composable
private fun WordBlock(flashcard: Flashcard) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = flashcard.word,
            fontFamily = bricolage,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 40.sp,
            lineHeight = 42.sp,
            letterSpacing = (-0.02).em,
            color = ink,
        )

        if (flashcard.phonetic.isNotBlank()) {
            Text(
                text = flashcard.phonetic,
                fontFamily = schibsted,
                fontSize = 15.sp,
                color = inkMuted,
            )
        }
    }
}

@Composable
private fun ExampleBlock(example: Example?, word: String) {
    if (example == null || example.text.isBlank()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = underlineFirstMatch(example.text, word),
            fontFamily = schibsted,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            color = ink,
        )

        if (example.translation.isNotBlank()) {
            Text(
                text = example.translation,
                fontFamily = schibsted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = inkSoft,
            )
        }
    }
}

@Composable
private fun ReferenceLine(flashcard: Flashcard) {
    val reference: String = listOf(flashcard.partOfSpeech, flashcard.meaning)
        .filter(String::isNotBlank)
        .joinToString(" · ")

    if (reference.isBlank()) return

    Text(
        text = reference,
        fontFamily = schibsted,
        fontSize = 12.sp,
        lineHeight = 19.sp,
        color = inkMuted,
    )
}

@Composable
private fun StatusLine(status: EnrichmentStatus) {
    when (status) {
        EnrichmentStatus.ENRICHED -> Unit
        EnrichmentStatus.PENDING -> Text(
            text = stringResource(R.string.library_status_pending),
            fontFamily = schibsted,
            fontSize = 13.sp,
            color = inkMuted,
        )
        EnrichmentStatus.FAILED -> Text(
            text = stringResource(R.string.library_status_failed),
            fontFamily = schibsted,
            fontSize = 13.sp,
            color = destructiveInk,
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
private fun FlashcardDetailScreenPreview() {
    HelloTheme {
        FlashcardDetailScreen(
            state = FlashcardDetailUiState(
                flashcard = Flashcard.empty(SystemClock).copy(
                    word = "aesthetic",
                    meaning = "concerned with beauty or the appreciation of beauty",
                    translation = "estético",
                    phonetic = "/esˈθetɪk/",
                    partOfSpeech = "adjective",
                    examples = listOf(
                        Example(
                            exampleId = "e1",
                            text = "The new building has a strong aesthetic.",
                            translation = "El nuevo edificio tiene una estética muy marcada.",
                            type = "usage",
                        ),
                    ),
                ),
                isLoading = false,
            ),
            onIntent = {},
        )
    }
}

package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.emm.domain.generation.EnrichmentFailureCause
import com.emm.domain.time.SystemClock
import com.emm.hello.R
import com.emm.hello.core.audio.AudioState
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.bricolage
import com.emm.hello.core.theme.cardHueFor
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
import com.emm.hello.core.ui.HSpeakerButton
import com.emm.hello.core.ui.HTopBar
import com.emm.hello.core.ui.underlineFirstMatch
import com.emm.hello.newfeatures.capture.messageRes
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val WORD_UTTERANCE_ID = "card_word"
private const val EXAMPLE_UTTERANCE_ID = "card_example"

@Composable
fun FlashcardDetailScreen(
    state: FlashcardDetailUiState,
    onIntent: (FlashcardDetailUiIntent) -> Unit,
    modifier: Modifier = Modifier,
    onSpeak: (String, String) -> Unit = { _, _ -> },
    onStopSpeech: () -> Unit = {},
    audioState: AudioState = AudioState(),
) {
    val hue: Color = cardHueFor(state.flashcard.id.value)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = hue,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            HTopBar(
                onBack = { onIntent(FlashcardDetailUiIntent.BackClicked) },
                actions = { DetailActions(onIntent = onIntent) },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = MaterialTheme.spacing.screenGutter)
                    .padding(bottom = 24.dp),
            ) {
                if (state.isLoading) {
                    LoadingBody(modifier = Modifier.weight(1f))
                } else {
                    CardBody(
                        flashcard = state.flashcard,
                        failedEnrichment = state.failedEnrichment,
                        onIntent = onIntent,
                        onSpeak = onSpeak,
                        onStopSpeech = onStopSpeech,
                        audioState = audioState,
                        modifier = Modifier.weight(1f),
                    )
                }
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
private fun DetailActions(onIntent: (FlashcardDetailUiIntent) -> Unit) {
    var isMenuExpanded: Boolean by remember { mutableStateOf(false) }

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

@Composable
private fun CardBody(
    flashcard: Flashcard,
    failedEnrichment: FailedEnrichment?,
    onIntent: (FlashcardDetailUiIntent) -> Unit,
    onSpeak: (String, String) -> Unit,
    onStopSpeech: () -> Unit,
    audioState: AudioState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        WordBlock(
            flashcard = flashcard,
            onSpeak = onSpeak,
            onStopSpeech = onStopSpeech,
            audioState = audioState,
        )

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

        ExampleBlock(
            example = flashcard.examples.firstOrNull(),
            word = flashcard.word,
            onSpeak = onSpeak,
            onStopSpeech = onStopSpeech,
            audioState = audioState,
        )

        ReferenceLine(flashcard = flashcard)

        CapturedInputLine(flashcard = flashcard)

        StatusLine(
            status = flashcard.enrichmentStatus,
            failedEnrichment = failedEnrichment,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun WordBlock(
    flashcard: Flashcard,
    onSpeak: (String, String) -> Unit,
    onStopSpeech: () -> Unit,
    audioState: AudioState,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
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

        HSpeakerButton(
            isSpeaking = audioState.isSpeaking(WORD_UTTERANCE_ID),
            onSpeak = { onSpeak(flashcard.word, WORD_UTTERANCE_ID) },
            onStop = onStopSpeech,
            enabled = audioState.isTtsReady && flashcard.word.isNotBlank(),
        )
    }
}

@Composable
private fun ExampleBlock(
    example: Example?,
    word: String,
    onSpeak: (String, String) -> Unit,
    onStopSpeech: () -> Unit,
    audioState: AudioState,
) {
    if (example == null || example.text.isBlank()) return

    val isSpeakingExample: Boolean = audioState.isSpeaking(EXAMPLE_UTTERANCE_ID)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = underlineFirstMatch(example.text, word),
                modifier = Modifier.weight(1f),
                fontFamily = schibsted,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                color = ink,
            )

            HSpeakerButton(
                isSpeaking = isSpeakingExample,
                onSpeak = { onSpeak(example.text, EXAMPLE_UTTERANCE_ID) },
                onStop = onStopSpeech,
                contentDescription = stringResource(
                    if (isSpeakingExample) {
                        R.string.stop_example_speech_desc
                    } else {
                        R.string.speak_example_desc
                    },
                ),
                tint = inkSoft,
                iconSize = 20.dp,
                buttonSize = 40.dp,
                enabled = audioState.isTtsReady,
            )
        }

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
private fun CapturedInputLine(flashcard: Flashcard) {
    val capturedInput: String = flashcard.capturedInput

    if (capturedInput.isBlank() || capturedInput.equals(flashcard.word, ignoreCase = true)) return

    Text(
        text = stringResource(R.string.card_detail_captured_input, capturedInput),
        fontFamily = schibsted,
        fontSize = 12.sp,
        lineHeight = 19.sp,
        color = inkMuted,
    )
}

@Composable
private fun StatusLine(
    status: EnrichmentStatus,
    failedEnrichment: FailedEnrichment?,
    onIntent: (FlashcardDetailUiIntent) -> Unit,
) {
    when (status) {
        EnrichmentStatus.ENRICHED -> Unit
        EnrichmentStatus.PENDING -> Text(
            text = stringResource(R.string.library_status_pending),
            fontFamily = schibsted,
            fontSize = 13.sp,
            color = inkMuted,
        )
        EnrichmentStatus.FAILED -> Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            Text(
                text = stringResource(R.string.library_status_failed),
                fontFamily = schibsted,
                fontSize = 13.sp,
                color = destructiveInk,
            )
            if (failedEnrichment != null) {
                FailedEnrichmentNotice(failedEnrichment = failedEnrichment, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun FailedEnrichmentNotice(
    failedEnrichment: FailedEnrichment,
    onIntent: (FlashcardDetailUiIntent) -> Unit,
) {
    Text(
        text = failedEnrichment.message(),
        style = MaterialTheme.typography.bodySmall,
        color = inkMuted,
    )

    when (failedEnrichment.action) {
        FailedEnrichmentAction.TryAgain -> HButton(
            text = stringResource(R.string.card_detail_try_again),
            onClick = { onIntent(FlashcardDetailUiIntent.TryAgainClicked) },
            variant = HButtonVariant.Primary,
            full = true,
        )
        FailedEnrichmentAction.WriteItMyself -> HButton(
            text = stringResource(R.string.card_detail_write_it_myself),
            onClick = { onIntent(FlashcardDetailUiIntent.EditFlashcard) },
            variant = HButtonVariant.Primary,
            full = true,
        )
        FailedEnrichmentAction.None -> Unit
    }
}

@Composable
private fun FailedEnrichment.message(): String = when (cause) {
    EnrichmentFailureCause.Technical -> stringResource(R.string.card_detail_failure_technical)
    EnrichmentFailureCause.AppCheckRejected -> stringResource(R.string.card_detail_failure_app_check_rejected)
    is EnrichmentFailureCause.WordProblem -> stringResource(cause.problem.messageRes())
    EnrichmentFailureCause.CreditsExhausted -> creditsMessage()
}

@Composable
private fun FailedEnrichment.creditsMessage(): String {
    val resetAt: Instant = creditsResetAt ?: return stringResource(R.string.card_detail_failure_credits_available)
    if (canRetry) return stringResource(R.string.card_detail_failure_credits_available)
    val availableAt: String = LocalDateTime
        .ofInstant(resetAt, ZoneId.systemDefault())
        .toLocalTime()
        .format(creditsResetFormatter)
    return stringResource(R.string.card_detail_failure_credits_until, availableAt)
}

private val creditsResetFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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

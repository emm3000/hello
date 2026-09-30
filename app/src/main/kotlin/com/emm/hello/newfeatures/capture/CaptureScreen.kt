package com.emm.hello.newfeatures.capture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.generation.GenerationRefusalCode
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.hello.R
import androidx.annotation.StringRes
import com.emm.hello.core.audio.SpeechRecognitionError
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.cardMint
import com.emm.hello.core.theme.hairline
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkSoft
import com.emm.hello.core.theme.schibsted
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HDropdownMenu
import com.emm.hello.core.ui.HFieldVariant
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HMenuItem
import java.time.LocalDateTime
import kotlinx.coroutines.launch

@Composable
fun CaptureScreen(
    state: CaptureUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onIntent: (CaptureUiIntent) -> Unit,
) {
    val snackbarScope = rememberCoroutineScope()
    val micPermissionDeniedMessage: String = stringResource(R.string.mic_permission_denied)
    val dictation: DictationState = rememberDictationState(
        onText = { voiceText -> onIntent(CaptureUiIntent.WordChanged(voiceText)) },
        onPermissionDenied = {
            snackbarScope.launch { snackbarHostState.showSnackbar(micPermissionDeniedMessage) }
        },
    )
    val dictationErrorMessage: String? = dictation.error?.let { stringResource(it.messageRes()) }

    LaunchedEffect(dictationErrorMessage) {
        if (dictationErrorMessage != null) {
            snackbarHostState.showSnackbar(dictationErrorMessage)
            dictation.onClearError()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = cardMint) {
        Box(modifier = Modifier.fillMaxSize()) {
            CaptureContent(
                state = state,
                isListening = dictation.isListening,
                micLevel = dictation.level,
                onNavigateBack = onNavigateBack,
                onMicToggle = dictation.onToggle,
                onIntent = onIntent,
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun CaptureContent(
    state: CaptureUiState,
    isListening: Boolean,
    micLevel: () -> Float,
    onNavigateBack: () -> Unit,
    onMicToggle: () -> Unit,
    onIntent: (CaptureUiIntent) -> Unit,
) {
    val wordFocusRequester: FocusRequester = remember { FocusRequester() }
    val translationFocusRequester: FocusRequester = remember { FocusRequester() }
    val meaningFocusRequester: FocusRequester = remember { FocusRequester() }
    val keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
    val newCaptureHighlight: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }
    val newestCaptureId: FlashcardId? = state.recentCaptures.firstOrNull()?.flashcardId
    var acknowledgedCaptureId: String? by rememberSaveable { mutableStateOf(newestCaptureId?.value) }

    LaunchedEffect(newestCaptureId) {
        if (newestCaptureId == null || newestCaptureId.value == acknowledgedCaptureId) return@LaunchedEffect
        acknowledgedCaptureId = newestCaptureId.value
        wordFocusRequester.requestFocus()
        keyboardController?.show()
        newCaptureHighlight.snapTo(1f)
        newCaptureHighlight.animateTo(0f, tween(durationMillis = NEW_CAPTURE_HIGHLIGHT_MILLIS))
    }

    LaunchedEffect(state.translationErrorRes) {
        if (state.translationErrorRes != null && state.isManual) {
            translationFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        CaptureHeader(onNavigateBack = onNavigateBack)

        CaptureDestination(state = state, onIntent = onIntent)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HInput(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(wordFocusRequester),
                    value = state.word,
                    onValueChange = { onIntent(CaptureUiIntent.WordChanged(it)) },
                    placeholder = stringResource(R.string.capture_placeholder),
                    errorMessage = state.wordErrorRes?.let { stringResource(it) },
                    variant = HFieldVariant.Underline,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (state.isManual) ImeAction.Next else ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { translationFocusRequester.requestFocus() },
                        onSend = { onIntent(CaptureUiIntent.Submit) },
                    ),
                )

                MicButton(
                    isListening = isListening,
                    level = micLevel,
                    onClick = onMicToggle,
                )
            }

            CaptureModeRow(state = state, onIntent = onIntent)

            if (state.isManual) {
                CaptureManualFields(
                    state = state,
                    translationFocusRequester = translationFocusRequester,
                    meaningFocusRequester = meaningFocusRequester,
                    onIntent = onIntent,
                )
            }

            if (state.recentCaptures.isNotEmpty()) {
                CaptureRecentList(
                    state = state,
                    newCaptureHighlight = { newCaptureHighlight.value },
                    onIntent = onIntent,
                )
            }

            if (state.failed > 0) {
                HButton(
                    text = stringResource(R.string.capture_retry),
                    onClick = { onIntent(CaptureUiIntent.RetryFailed) },
                    variant = HButtonVariant.Text,
                )
            }
        }

        HButton(
            text = stringResource(R.string.capture_save),
            onClick = { onIntent(CaptureUiIntent.Submit) },
            enabled = state.canSubmit,
            isLoading = state.isSaving,
            variant = HButtonVariant.Primary,
            full = true,
        )
    }
}

@Composable
private fun CaptureDestination(
    state: CaptureUiState,
    onIntent: (CaptureUiIntent) -> Unit,
) {
    val targetDeck: Deck = state.targetDeck ?: return
    if (state.decks.size <= 1) return

    Box {
        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clickable(
                    onClickLabel = stringResource(R.string.capture_deck_picker_content_description),
                    onClick = { onIntent(CaptureUiIntent.DeckPickerOpened) },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.capture_deck_label),
                fontFamily = schibsted,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = inkSoft,
            )

            Text(
                text = targetDeck.name,
                fontFamily = schibsted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = ink,
            )

            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = inkSoft,
                modifier = Modifier.size(MaterialTheme.spacing.lg),
            )
        }

        HDropdownMenu(
            expanded = state.isDeckPickerOpen,
            onDismissRequest = { onIntent(CaptureUiIntent.DeckPickerDismissed) },
            items = state.decks.map { candidate ->
                HMenuItem(
                    label = candidate.name,
                    onClick = { onIntent(CaptureUiIntent.DeckSelected(candidate.id)) },
                    icon = if (candidate.id == targetDeck.id) Icons.Outlined.Check else null,
                )
            },
        )
    }
}

@Composable
private fun CaptureModeRow(
    state: CaptureUiState,
    onIntent: (CaptureUiIntent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                if (state.isManual) R.string.capture_mode_manual_label else R.string.capture_mode_ai_label,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = inkSoft,
        )

        Spacer(modifier = Modifier.weight(1f))

        HButton(
            onClick = { onIntent(CaptureUiIntent.ManualModeSelected(!state.isManual)) },
            variant = HButtonVariant.Text,
            enabled = !state.isSaving,
        ) {
            Text(
                text = stringResource(
                    if (state.isManual) R.string.capture_mode_ai_action else R.string.capture_mode_manual_action,
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}

@Composable
private fun CaptureManualFields(
    state: CaptureUiState,
    translationFocusRequester: FocusRequester,
    meaningFocusRequester: FocusRequester,
    onIntent: (CaptureUiIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HInput(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(translationFocusRequester),
            value = state.translation,
            onValueChange = { onIntent(CaptureUiIntent.TranslationChanged(it)) },
            label = stringResource(R.string.capture_manual_translation_label),
            placeholder = stringResource(R.string.capture_manual_translation_placeholder),
            errorMessage = state.translationErrorRes?.let { stringResource(it) },
            variant = HFieldVariant.Underline,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { meaningFocusRequester.requestFocus() }),
        )

        HInput(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(meaningFocusRequester),
            value = state.meaning,
            onValueChange = { onIntent(CaptureUiIntent.MeaningChanged(it)) },
            label = stringResource(R.string.capture_manual_meaning_label),
            placeholder = stringResource(R.string.capture_manual_meaning_placeholder),
            variant = HFieldVariant.Underline,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onIntent(CaptureUiIntent.Submit) }),
        )
    }
}

@Composable
private fun CaptureHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.capture_title).uppercase(),
            fontFamily = schibsted,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            letterSpacing = 0.12.em,
            color = inkSoft,
        )

        Spacer(modifier = Modifier.weight(1f))

        HButton(
            text = stringResource(R.string.capture_done),
            onClick = onNavigateBack,
            variant = HButtonVariant.Text,
        )
    }
}

@Composable
private fun CaptureRecentList(
    state: CaptureUiState,
    newCaptureHighlight: () -> Float,
    onIntent: (CaptureUiIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.capture_recent_label),
            fontFamily = schibsted,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = inkSoft,
        )

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
            state.recentCaptures.forEachIndexed { index, capture ->
                val isNewest: Boolean = index == 0
                CaptureRecentRow(
                    capture = capture,
                    isOnline = state.isOnline,
                    isNewest = isNewest,
                    highlight = if (isNewest) newCaptureHighlight else noHighlight,
                    onClick = { onIntent(CaptureUiIntent.RecentCaptureClicked(capture.flashcardId)) },
                )
            }
        }
    }
}

@Composable
private fun CaptureRecentRow(
    capture: RecentCapture,
    isOnline: Boolean,
    isNewest: Boolean,
    highlight: () -> Float,
    onClick: () -> Unit,
) {
    val statusLabel: String = stringResource(capture.status.labelRes(isOnline = isOnline))
    val rowDescription: String = stringResource(R.string.capture_recent_row_description, capture.word, statusLabel)

    Column(
        modifier = Modifier
            .bleedHorizontally(MaterialTheme.spacing.sm)
            .fillMaxWidth()
            .clip(MaterialTheme.helloShapes.control)
            .drawBehind { drawRect(color = hairline.copy(alpha = hairline.alpha * highlight())) }
            .clickable(onClickLabel = stringResource(R.string.capture_recent_open_card), onClick = onClick)
            .semantics {
                contentDescription = rowDescription
                if (isNewest) liveRegion = LiveRegionMode.Polite
            }
            .padding(horizontal = MaterialTheme.spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = capture.word,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {},
                style = MaterialTheme.typography.titleMedium,
                color = ink,
            )
            Text(
                text = statusLabel,
                modifier = Modifier.clearAndSetSemantics {},
                style = MaterialTheme.typography.bodySmall,
                color = inkSoft,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = inkSoft,
                modifier = Modifier.size(MaterialTheme.spacing.lg),
            )
        }

        CaptureFailureMessage(capture = capture)
    }
}

private fun Modifier.bleedHorizontally(bleed: Dp): Modifier = layout { measurable, constraints ->
    val bleedPx: Int = bleed.roundToPx()
    val widened: Constraints = if (constraints.hasBoundedWidth) {
        constraints.copy(maxWidth = constraints.maxWidth + bleedPx * 2)
    } else {
        constraints
    }
    val placeable: Placeable = measurable.measure(widened)
    val width: Int = constraints.constrainWidth(placeable.width - bleedPx * 2)
    layout(width, placeable.height) {
        placeable.place(-bleedPx, 0)
    }
}

private val noHighlight: () -> Float = { 0f }

private const val NEW_CAPTURE_HIGHLIGHT_MILLIS: Int = 1500

@Composable
private fun CaptureFailureMessage(capture: RecentCapture) {
    if (capture.status != EnrichmentStatus.FAILED) return
    val failure: EnrichmentFailure = capture.failure ?: return
    val message: String = failure.code?.let { code -> stringResource(code.messageRes()) }
        ?: failure.reason
        ?: return

    Text(
        text = message,
        fontFamily = schibsted,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = inkSoft,
    )
}

private fun EnrichmentStatus.labelRes(isOnline: Boolean): Int = when (this) {
    EnrichmentStatus.PENDING ->
        if (isOnline) R.string.capture_status_preparing else R.string.capture_status_waiting_for_connection
    EnrichmentStatus.ENRICHED -> R.string.capture_status_ready
    EnrichmentStatus.FAILED -> R.string.capture_status_failed
}

private val previewStarterDeck: Deck = Deck(
    id = "deck-1".toDeckId(),
    name = "Primeras palabras",
    description = "",
    createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
    cards = emptyList(),
    cardsCount = 0L,
)

private val previewInterviewDeck: Deck = Deck(
    id = "deck-2".toDeckId(),
    name = "Job interview",
    description = "",
    createdAt = LocalDateTime.of(2026, 6, 1, 0, 0),
    cards = emptyList(),
    cardsCount = 0L,
)

@PreviewLightDark
@Composable
private fun CaptureScreenPreview() {
    HelloTheme {
        CaptureScreen(
            state = CaptureUiState(
                word = "compelling",
                targetDeck = previewStarterDeck,
                decks = listOf(previewStarterDeck, previewInterviewDeck),
                recentCaptures = listOf(
                    RecentCapture(
                        flashcardId = "1".toFlashcardId(),
                        deckId = previewStarterDeck.id,
                        word = "borrow",
                        status = EnrichmentStatus.ENRICHED,
                    ),
                    RecentCapture(
                        flashcardId = "2".toFlashcardId(),
                        deckId = previewStarterDeck.id,
                        word = "compelling",
                        status = EnrichmentStatus.PENDING,
                    ),
                    RecentCapture(
                        flashcardId = "3".toFlashcardId(),
                        deckId = previewStarterDeck.id,
                        word = "asdkjqwe",
                        status = EnrichmentStatus.FAILED,
                        failure = EnrichmentFailure(GenerationRefusalCode.Unintelligible, null),
                    ),
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onIntent = {},
        )
    }
}

@StringRes
private fun SpeechRecognitionError.messageRes(): Int {
    return when (this) {
        SpeechRecognitionError.NotHeard -> R.string.speech_error_not_heard
        SpeechRecognitionError.NoConnection -> R.string.speech_error_no_connection
        SpeechRecognitionError.MicrophoneUnavailable -> R.string.speech_error_microphone_unavailable
        SpeechRecognitionError.PermissionMissing -> R.string.speech_error_permission_missing
        SpeechRecognitionError.RecognizerBusy -> R.string.speech_error_recognizer_busy
        SpeechRecognitionError.Unavailable -> R.string.speech_error_unavailable
    }
}

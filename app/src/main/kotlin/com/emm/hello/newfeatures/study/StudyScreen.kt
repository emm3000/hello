package com.emm.hello.newfeatures.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.EvaluationMode
import com.emm.domain.flashcard.GeneratedStudyCard
import com.emm.domain.flashcard.StudyCardType
import com.emm.domain.study.ReviewGrade
import com.emm.hello.R
import com.emm.hello.core.audio.TextToSpeechManager
import com.emm.hello.core.audio.rememberTextToSpeechManager
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HProgressBar
import com.emm.hello.core.ui.HSeparator

private const val CARD_TRANSITION_DURATION_MS = 350
private const val CARD_TRANSITION_DIVISOR = 3
private const val CARD_EXIT_FADE_DURATION_MS = 250
private const val ANSWER_BUTTON_FADE_DURATION_MS = 200
private const val ANSWER_BUTTONS_PLACEHOLDER_HEIGHT_DP = 104
private const val PHONETIC_SEPARATOR_WIDTH_FRACTION = 0.4f
private const val MEANING_SEPARATOR_WIDTH_FRACTION = 0.5f

@Composable
fun StudyScreen(
    modifier: Modifier = Modifier,
    onBackRequested: () -> Unit = {},
    onFinishDialogDismissed: () -> Unit = {},
    onReviewAnswer: (StudySessionItem?, ReviewGrade) -> Unit = { _, _ -> },
    state: StudyUiState = StudyUiState(),
    showFinishDialog: Boolean = false,
) {
    val tts: TextToSpeechManager = rememberTextToSpeechManager()
    val isSpeaking by tts.isSpeaking.collectAsStateWithLifecycle()
    val ttsReady by tts.isReady.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    val prevStudyItem = remember { mutableStateOf(state.currentItem) }
    var cardFace by remember { mutableStateOf(CardFace.Front) }

    val progress = if (state.totalCount > 0) {
        state.reviewedCount.toFloat() / state.totalCount.toFloat()
    } else {
        0f
    }

    LaunchedEffect(state.currentItem?.studyCard?.cardId) { cardFace = CardFace.Front }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(R.string.study_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            HBadge(
                                label = "${state.reviewedCount}/${state.totalCount}",
                                variant = BadgeVariant.Secondary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackRequested) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.exit_session_desc),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            // ── Progress bar (shadcn-style) ─────────────────────────────────
            HProgressBar(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // ── Hint: "Toca para revelar" ───────────────────────────────
                Text(
                    text = if (cardFace == CardFace.Front) stringResource(R.string.tap_to_reveal) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )

                // ── Animated card with slide transition between flashcards ──
                AnimatedContent(
                    targetState = state.currentItem,
                    transitionSpec = {
                        (
                            slideInHorizontally(tween(CARD_TRANSITION_DURATION_MS)) {
                                it / CARD_TRANSITION_DIVISOR
                            } +
                                fadeIn(tween(CARD_TRANSITION_DURATION_MS))
                            )
                            .togetherWith(
                                slideOutHorizontally(tween(CARD_TRANSITION_DURATION_MS)) {
                                    -it / CARD_TRANSITION_DIVISOR
                                } + fadeOut(tween(CARD_EXIT_FADE_DURATION_MS))
                            )
                    },
                    label = "card_transition",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) { item ->
                    FlippableCard(
                        cardFace = cardFace,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            cardFace = it.next
                        },
                        progress = progress,
                        onFinished = { prevStudyItem.value = state.currentItem },
                        modifier = Modifier.fillMaxSize(),
                        frontContent = {
                            FlashcardFrontContent(
                                prompt = item?.studyCard?.prompt ?: item?.flashcard?.word.orEmpty(),
                                phonetic = if (item?.studyCard?.sourceField == "word") {
                                    item.flashcard.phonetic
                                } else {
                                    ""
                                },
                                cardType = item?.studyCard?.cardType,
                            )
                        },
                        backContent = {
                            val currentItem = prevStudyItem.value
                            FlashcardBackContent(
                                card = currentItem?.flashcard,
                                studyCard = currentItem?.studyCard,
                                isSpeaking = isSpeaking,
                                ttsReady = ttsReady,
                                onStop = { tts.stop() },
                                onSpeak = {
                                    if (ttsReady) {
                                        tts.speak(state.currentItem?.flashcard?.word.orEmpty())
                                    }
                                },
                            )
                        },
                    )
                }

                // ── Answer buttons ──────────────────────────────────────────
                AnimatedContent(
                    targetState = cardFace == CardFace.Back,
                    transitionSpec = {
                        fadeIn(tween(ANSWER_BUTTON_FADE_DURATION_MS)) togetherWith
                            fadeOut(tween(ANSWER_BUTTON_FADE_DURATION_MS))
                    },
                    label = "answer_buttons",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) { showButtons ->
                    if (showButtons) {
                        AnswerButtons { grade ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReviewAnswer(state.currentItem, grade)
                        }
                    } else {
                        Spacer(Modifier.height(ANSWER_BUTTONS_PLACEHOLDER_HEIGHT_DP.dp))
                    }
                }
            }
        }
    }

    if (showFinishDialog) {
        HAlertDialog(
            title = stringResource(R.string.session_completed_title),
            description = stringResource(R.string.session_completed_desc, state.totalCount),
            icon = Icons.Outlined.Check,
            confirmText = stringResource(R.string.back),
            cancelText = null,
            onConfirm = {
                onFinishDialogDismissed()
            },
            onDismiss = {
                onFinishDialogDismissed()
            },
        )
    }
}

// ── Front content ────────────────────────────────────────────────────────────

@Composable
private fun FlashcardFrontContent(
    prompt: String,
    phonetic: String,
    cardType: StudyCardType?,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = prompt,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (cardType != null) {
                Spacer(Modifier.height(12.dp))
                HBadge(
                    label = cardType.label,
                    variant = BadgeVariant.Outline,
                )
            }
            if (phonetic.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                HSeparator(modifier = Modifier.fillMaxWidth(PHONETIC_SEPARATOR_WIDTH_FRACTION))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = phonetic,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Back content ─────────────────────────────────────────────────────────────

@Composable
private fun FlashcardBackContent(
    card: Flashcard?,
    studyCard: GeneratedStudyCard?,
    isSpeaking: Boolean,
    ttsReady: Boolean,
    onStop: () -> Unit = {},
    onSpeak: () -> Unit = {},
) {
    val primaryText = studyCard?.expectedAnswer ?: card?.translation.orEmpty()
    val secondaryText = when {
        studyCard?.hint?.isNotBlank() == true -> studyCard.hint
        studyCard?.explanation?.isNotBlank() == true -> studyCard.explanation
        card?.meaning?.isNotBlank() == true -> card.meaning
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // ── Translation (primary answer) ────────────────────────────────
        Text(
            text = primaryText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(12.dp))
        HSeparator(modifier = Modifier.fillMaxWidth(MEANING_SEPARATOR_WIDTH_FRACTION))
        Spacer(Modifier.height(12.dp))

        // ── Meaning (secondary info) ────────────────────────────────────
        if (secondaryText.isNotBlank()) {
            Text(
                text = secondaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── TTS button ──────────────────────────────────────────────────
        HButton(
            text = if (isSpeaking) stringResource(R.string.stop_speech_desc) else stringResource(R.string.speak_desc),
            onClick = { if (isSpeaking) onStop() else onSpeak() },
            enabled = ttsReady,
            variant = ButtonVariant.Ghost,
            leadingIcon = Icons.AutoMirrored.Filled.VolumeUp,
        )
    }
}

// ── Answer buttons with icons ────────────────────────────────────────────────

@Composable
private fun AnswerButtons(
    modifier: Modifier = Modifier,
    onReviewAnswer: (ReviewGrade) -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HButton(
                text = stringResource(R.string.grade_again),
                onClick = { onReviewAnswer(ReviewGrade.AGAIN) },
                variant = ButtonVariant.Destructive,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Outlined.Refresh,
            )
            HButton(
                text = stringResource(R.string.grade_hard),
                onClick = { onReviewAnswer(ReviewGrade.HARD) },
                variant = ButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Outlined.Warning,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HButton(
                text = stringResource(R.string.grade_good),
                onClick = { onReviewAnswer(ReviewGrade.GOOD) },
                variant = ButtonVariant.Default,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Rounded.CheckCircle,
            )
            HButton(
                text = stringResource(R.string.grade_easy),
                onClick = { onReviewAnswer(ReviewGrade.EASY) },
                variant = ButtonVariant.Outline,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Rounded.Bolt,
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun StudyScreenPreview() {
    HelloTheme {
        StudyScreen(
            state = StudyUiState(
                currentItem = StudySessionItem(
                    flashcard = Flashcard(
                        id = "1",
                        word = "Serendipity",
                        meaning = "The occurrence of events by chance in a happy way",
                        translation = "Casualidad afortunada",
                        examples = listOf(),
                        phonetic = "/ˌserənˈdɪpɪti/",
                        review = FlashcardReview.Empty,
                    ),
                    studyCard = GeneratedStudyCard(
                        cardId = "study-card-1",
                        cardType = StudyCardType.Recognition,
                        prompt = "Serendipity",
                        expectedAnswer = "Casualidad afortunada",
                        evaluationMode = EvaluationMode.ManualSelfCheck,
                    ),
                ),
                reviewedCount = 3,
                totalCount = 10,
            ),
        )
    }
}

private val StudyCardType.label: String
    get() = when (this) {
        StudyCardType.Recognition -> "Recognition"
        StudyCardType.Production -> "Production"
        StudyCardType.Cloze -> "Cloze"
        StudyCardType.Form -> "Form"
    }

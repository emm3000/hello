package com.emm.hello.newfeatures.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.study.ReviewGrade
import com.emm.domain.time.SystemClock
import com.emm.hello.R
import com.emm.hello.core.audio.TextToSpeechManager
import com.emm.hello.core.audio.rememberTextToSpeechManager
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberBad
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberDivider
import com.emm.hello.core.theme.emberElev
import com.emm.hello.core.theme.emberFaint
import com.emm.hello.core.theme.emberHint
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberOnBg
import com.emm.hello.core.theme.emberSurface
import com.emm.hello.core.theme.emberWarn
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.instrumentSerif
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonSize
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HCard
import com.emm.hello.core.ui.HEmptyState
import com.emm.hello.core.ui.HIconButton
import com.emm.hello.core.ui.HLoadingSpinner

private const val CARD_TRANSITION_DURATION_MS = 220
private const val CARD_EXIT_FADE_DURATION_MS = 160
private const val CARD_ENTER_SCALE = 0.96f
private const val CARD_EXIT_SCALE = 0.92f
private val studyDockMinHeight = 220.dp
private val recallPromptFontSize = 48.sp
private val recallPromptLineHeight = 54.sp
private val gradeAnswerFontSize = 44.sp
private val gradeAnswerLineHeight = 50.sp
private val gradeChipDashWidth = 6.dp
private val gradeChipDashGap = 4.dp

private data class CardViewState(
    val cardFace: CardFace,
    val progress: Float,
)

private data class AudioState(
    val isSpeaking: Boolean,
    val ttsReady: Boolean,
)

private data class StudyDockCallbacks(
    val onRevealAnswer: () -> Unit,
    val onReviewAnswer: (ReviewGrade) -> Unit,
    val onCreateCard: () -> Unit,
    val onRetryLoad: () -> Unit,
)

@Composable
fun StudyScreen(
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {},
    onFinishDialogDismissed: () -> Unit = {},
    onReviewAnswer: (StudySessionItem?, ReviewGrade) -> Unit = { _, _ -> },
    onCreateCard: () -> Unit = {},
    onGradeHintDismissed: () -> Unit = {},
    onRetryLoad: () -> Unit = {},
    state: StudyUiState = StudyUiState(),
    showFinishDialog: Boolean = false,
) {
    // System/predictive back and the on-screen X share one exit path. Every grade is persisted
    // the moment it is given, so leaving never loses progress and needs no confirmation.
    BackHandler(onBack = onExit)
    val tts: TextToSpeechManager = rememberTextToSpeechManager()
    val isSpeaking by tts.isSpeaking.collectAsStateWithLifecycle()
    val ttsReady by tts.isReady.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    var cardFace by remember { mutableStateOf(CardFace.Front) }

    val progress = if (state.totalCount > 0) {
        state.reviewedCount.toFloat() / state.totalCount.toFloat()
    } else {
        0f
    }

    LaunchedEffect(state.currentItem?.flashcardId) {
        cardFace = CardFace.Front
    }

    val currentItem = state.currentItem
    val sessionStage = remember(state.isLoading, state.loadError, state.currentItem, cardFace) {
        when {
            state.isLoading -> StudyStage.Loading
            state.loadError != null -> StudyStage.Error
            state.currentItem == null -> StudyStage.Empty
            cardFace == CardFace.Front -> StudyStage.Recall
            else -> StudyStage.Grade
        }
    }

    val sessionInProgress = sessionStage == StudyStage.Recall || sessionStage == StudyStage.Grade
    // The counter names the card on screen ("3/10"), not how many were already graded.
    val cardPosition = if (sessionInProgress) {
        minOf(state.reviewedCount + 1, state.totalCount)
    } else {
        state.reviewedCount
    }
    val stateLabel = when (sessionStage) {
        StudyStage.Recall -> stringResource(R.string.study_state_label_recall)
        StudyStage.Grade -> stringResource(R.string.study_state_label_grade)
        else -> null
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = emberBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            StudyTop(
                progress = progress,
                currentCount = cardPosition,
                totalCount = state.totalCount,
                stateLabel = stateLabel,
                onClose = onExit,
                showCounter = sessionInProgress,
            )

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                color = emberBg,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StudyCanvas(
                        sessionStage = sessionStage,
                        currentItem = currentItem,
                        cardViewState = CardViewState(
                            cardFace = cardFace,
                            progress = progress,
                        ),
                        audioState = AudioState(
                            isSpeaking = isSpeaking,
                            ttsReady = ttsReady,
                        ),
                        onFlipCard = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            cardFace = it.next
                        },
                        onGradeSwipe = { grade ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReviewAnswer(state.currentItem, grade)
                        },
                        onStop = { tts.stop() },
                        onSpeak = {
                            if (ttsReady) {
                                tts.speak(state.currentItem?.word.orEmpty())
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )

                    StudyActionDock(
                        sessionStage = sessionStage,
                        intervalPreviews = state.intervalPreviews,
                        isGradeHintVisible = state.isGradeHintVisible,
                        callbacks = StudyDockCallbacks(
                            onRevealAnswer = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                cardFace = CardFace.Back
                            },
                            onReviewAnswer = { grade ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReviewAnswer(state.currentItem, grade)
                            },
                            onCreateCard = onCreateCard,
                            onRetryLoad = onRetryLoad,
                        ),
                        onGradeHintDismissed = onGradeHintDismissed,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showFinishDialog) {
        SessionFinishedDialog(
            totalCount = state.totalCount,
            onDismiss = onFinishDialogDismissed,
        )
    }
}

@Composable
private fun SessionFinishedDialog(
    totalCount: Int,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = emberElev,
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Image(
                    painter = painterResource(R.drawable.mascot_celebrate),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(104.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.session_completed_eyebrow).uppercase(),
                    fontFamily = geistMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.12.em,
                    color = emberMuted,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.session_completed_title),
                    fontFamily = instrumentSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 44.sp,
                    lineHeight = (44 * 1.04f).sp,
                    letterSpacing = (-0.5).sp,
                    color = emberOnBg,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.session_completed_desc, totalCount),
                    fontFamily = instrumentSerif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp,
                    color = emberMuted,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(emberSurface, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = totalCount.toString(),
                        fontFamily = geistMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        color = emberAccent,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.session_completed_stat_label).uppercase(),
                        fontFamily = geistMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 0.12.em,
                        color = emberMuted,
                    )
                }
                Spacer(Modifier.height(24.dp))
                HButton(
                    text = stringResource(R.string.session_completed_cta),
                    onClick = onDismiss,
                    variant = HButtonVariant.Accent,
                    full = true,
                )
            }
        }
    }
}

private enum class StudyStage {
    Loading,
    Error,
    Empty,
    Recall,
    Grade,
}

@Composable
private fun StudyCanvas(
    sessionStage: StudyStage,
    currentItem: StudySessionItem?,
    cardViewState: CardViewState,
    audioState: AudioState,
    onFlipCard: (CardFace) -> Unit,
    onGradeSwipe: (ReviewGrade) -> Unit,
    onStop: () -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (sessionStage) {
            StudyStage.Loading -> StudyLoadingState()
            StudyStage.Error -> StudyErrorState()
            StudyStage.Empty -> StudyEmptyState()
            StudyStage.Recall,
            StudyStage.Grade -> StudyCardStage(
                currentItem = currentItem,
                cardViewState = cardViewState,
                audioState = audioState,
                gradeEnabled = sessionStage == StudyStage.Grade,
                onFlipCard = onFlipCard,
                onGradeSwipe = onGradeSwipe,
                onStop = onStop,
                onSpeak = onSpeak,
            )
        }
    }
}

@Composable
private fun StudyCardStage(
    currentItem: StudySessionItem?,
    cardViewState: CardViewState,
    audioState: AudioState,
    gradeEnabled: Boolean,
    onFlipCard: (CardFace) -> Unit,
    onGradeSwipe: (ReviewGrade) -> Unit,
    onStop: () -> Unit,
    onSpeak: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentItem,
            transitionSpec = {
                val enterSpec = tween<Float>(
                    durationMillis = CARD_TRANSITION_DURATION_MS,
                    easing = FastOutSlowInEasing,
                )
                val exitSpec = tween<Float>(
                    durationMillis = CARD_EXIT_FADE_DURATION_MS,
                    easing = FastOutSlowInEasing,
                )
                (fadeIn(enterSpec) + scaleIn(initialScale = CARD_ENTER_SCALE, animationSpec = enterSpec))
                    .togetherWith(
                        fadeOut(exitSpec) + scaleOut(targetScale = CARD_EXIT_SCALE, animationSpec = exitSpec)
                    )
            },
            label = "card_transition",
            modifier = Modifier.fillMaxSize(),
        ) { item ->
            FlippableCard(
                cardFace = cardViewState.cardFace,
                onClick = onFlipCard,
                progress = cardViewState.progress,
                gradeEnabled = gradeEnabled,
                onGradeSwipe = onGradeSwipe,
                modifier = Modifier.fillMaxSize(),
                frontContent = {
                    FlashcardFront(
                        word = item?.word.orEmpty(),
                        phonetic = item?.phonetic.orEmpty(),
                    )
                },
                backContent = {
                    item?.let { FlashcardBack(item = it) }
                },
            )
        }
        Text(
            text = stringResource(R.string.study_direction_en_to_es),
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 0.12.em,
            color = emberMuted,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
        TtsFloatingButton(
            audioState = audioState,
            onSpeak = onSpeak,
            onStop = onStop,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
        )
    }
}

@Composable
private fun TtsFloatingButton(
    audioState: AudioState,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSpeaking = audioState.isSpeaking
    val description = stringResource(
        if (isSpeaking) R.string.stop_speech_desc else R.string.speak_desc,
    )
    HIconButton(
        icon = if (isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
        contentDescription = description,
        onClick = { if (isSpeaking) onStop() else onSpeak() },
        modifier = modifier,
        tint = emberOnBg,
        enabled = audioState.ttsReady,
    )
}

@Composable
private fun StudyActionDock(
    sessionStage: StudyStage,
    intervalPreviews: Map<ReviewGrade, Long>,
    isGradeHintVisible: Boolean,
    callbacks: StudyDockCallbacks,
    onGradeHintDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = sessionStage,
        transitionSpec = {
            fadeIn(tween(CARD_TRANSITION_DURATION_MS)) togetherWith
                fadeOut(tween(CARD_EXIT_FADE_DURATION_MS)) using
                SizeTransform(clip = false) { _, _ ->
                    spring(stiffness = Spring.StiffnessMediumLow)
                }
        },
        label = "study_action_dock",
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = studyDockMinHeight),
    ) { stage ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (stage) {
                StudyStage.Loading -> Unit

                StudyStage.Error -> {
                    HButton(
                        text = stringResource(R.string.study_error_retry),
                        onClick = callbacks.onRetryLoad,
                        variant = HButtonVariant.Accent,
                        size = HButtonSize.Lg,
                        full = true,
                    )
                }

                StudyStage.Empty -> {
                    HButton(
                        text = stringResource(R.string.study_empty_create_card_cta),
                        onClick = callbacks.onCreateCard,
                        variant = HButtonVariant.Secondary,
                        size = HButtonSize.Lg,
                        full = true,
                    )
                }

                StudyStage.Recall -> {
                    HButton(
                        text = stringResource(R.string.study_recall_cta_reveal),
                        onClick = callbacks.onRevealAnswer,
                        variant = HButtonVariant.Accent,
                        size = HButtonSize.Lg,
                        full = true,
                    )
                }

                StudyStage.Grade -> {
                    if (isGradeHintVisible) {
                        GradeHintCard(onDismiss = onGradeHintDismissed)
                    }
                    AnswerButtons(
                        intervalPreviews = intervalPreviews,
                        onReviewAnswer = callbacks.onReviewAnswer,
                    )
                    Text(
                        text = stringResource(R.string.study_grade_swipe_hint),
                        fontFamily = geistMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.12.em,
                        color = emberFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyEmptyState() {
    val srsConceptLine = stringResource(R.string.onboarding_srs_concept)
    val studyEmptyBody = stringResource(R.string.study_empty_body)
    HEmptyState(
        modifier = Modifier.fillMaxSize(),
        headline = stringResource(R.string.study_empty_headline),
        body = "$srsConceptLine\n\n$studyEmptyBody",
        glyph = {
            Image(
                painter = painterResource(R.drawable.mascot_rest),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
            )
        },
    )
}

@Composable
private fun StudyLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        HLoadingSpinner(color = emberAccent)
    }
}

@Composable
private fun StudyErrorState() {
    HEmptyState(
        modifier = Modifier.fillMaxSize(),
        headline = stringResource(R.string.study_error_headline),
        body = stringResource(R.string.study_error_body),
    )
}

@Composable
private fun FlashcardFront(word: String, phonetic: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 64.dp),
        ) {
            Text(
                text = word,
                fontFamily = instrumentSerif,
                fontWeight = FontWeight.Normal,
                fontSize = recallPromptFontSize,
                lineHeight = recallPromptLineHeight,
                letterSpacing = (-0.5).sp,
                color = emberOnBg,
                textAlign = TextAlign.Center,
            )
            if (phonetic.isNotBlank()) {
                Text(
                    text = phonetic,
                    fontFamily = geistMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = emberHint,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun FlashcardBack(item: StudySessionItem) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.study_grade_answer_label_recognition),
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 0.12.em,
            color = emberMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = item.translation,
            fontFamily = instrumentSerif,
            fontWeight = FontWeight.Normal,
            fontSize = gradeAnswerFontSize,
            lineHeight = gradeAnswerLineHeight,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center,
            color = emberOnBg,
        )
        if (item.phonetic.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.phonetic,
                fontFamily = geistMono,
                fontSize = 13.sp,
                color = emberHint,
                textAlign = TextAlign.Center,
            )
        }
        if (item.meaning.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = item.meaning,
                fontFamily = instrumentSerif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = emberMuted,
                textAlign = TextAlign.Center,
            )
        }
        if (item.irregularForms.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.study_related_forms_inline, item.irregularForms.joinToString()),
                fontFamily = geistMono,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                letterSpacing = 0.08.em,
                color = emberMuted,
                textAlign = TextAlign.Center,
            )
        }
        if (item.usagePattern.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = item.usagePattern,
                fontFamily = geist,
                fontWeight = FontWeight.Normal,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                color = emberMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AnswerButtons(
    modifier: Modifier = Modifier,
    intervalPreviews: Map<ReviewGrade, Long> = emptyMap(),
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
            GradeChip(
                grade = ReviewGrade.AGAIN,
                intervalDays = intervalPreviews[ReviewGrade.AGAIN],
                enabled = true,
                onClick = { onReviewAnswer(ReviewGrade.AGAIN) },
                modifier = Modifier.weight(1f),
            )
            GradeChip(
                grade = ReviewGrade.HARD,
                intervalDays = intervalPreviews[ReviewGrade.HARD],
                enabled = true,
                onClick = { onReviewAnswer(ReviewGrade.HARD) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GradeChip(
                grade = ReviewGrade.GOOD,
                intervalDays = intervalPreviews[ReviewGrade.GOOD],
                enabled = true,
                onClick = { onReviewAnswer(ReviewGrade.GOOD) },
                modifier = Modifier.weight(1f),
            )
            GradeChip(
                grade = ReviewGrade.EASY,
                intervalDays = intervalPreviews[ReviewGrade.EASY],
                enabled = true,
                onClick = { onReviewAnswer(ReviewGrade.EASY) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GradeChip(
    grade: ReviewGrade,
    intervalDays: Long?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = gradeAccentColor(grade)
    val labelRes = gradeLabelRes(grade)
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (enabled) emberDivider else emberDivider
    val containerColor = if (enabled) emberSurface else Color.Transparent
    val labelColor = if (enabled) accent else emberFaint
    val intervalColor = if (enabled) emberMuted else emberFaint
    val cornerRadius = 14.dp
    val chipModifier = modifier
        .heightIn(min = gradeChipMinHeight)
        .clip(shape)
        .background(containerColor, shape)
        .then(
            if (enabled) {
                Modifier.border(1.dp, borderColor, shape)
            } else {
                Modifier.dashedBorder(
                    color = borderColor,
                    cornerRadius = cornerRadius,
                    dashWidth = gradeChipDashWidth,
                    dashGap = gradeChipDashGap,
                )
            }
        )
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 14.dp)

    Column(
        modifier = chipModifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(labelRes),
            fontFamily = geist,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = labelColor,
        )
        Text(
            text = formatInterval(intervalDays),
            fontFamily = geistMono,
            fontWeight = FontWeight.Normal,
            fontSize = 10.5.sp,
            letterSpacing = 0.08.em,
            color = intervalColor,
        )
    }
}

@Composable
private fun gradeAccentColor(grade: ReviewGrade): Color = when (grade) {
    ReviewGrade.AGAIN -> emberBad
    ReviewGrade.HARD -> emberWarn
    ReviewGrade.GOOD -> emberOnBg
    ReviewGrade.EASY -> emberAccent
}

private fun gradeLabelRes(grade: ReviewGrade): Int = when (grade) {
    ReviewGrade.AGAIN -> R.string.grade_again
    ReviewGrade.HARD -> R.string.grade_hard
    ReviewGrade.GOOD -> R.string.grade_good
    ReviewGrade.EASY -> R.string.grade_easy
}

private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    dashWidth: Dp,
    dashGap: Dp,
): Modifier = this.then(
    Modifier.drawBehind {
        val stroke = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dashWidth.toPx(), dashGap.toPx()),
                phase = 0f,
            ),
        )
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style = stroke,
        )
    },
)

@Composable
private fun formatInterval(days: Long?): String {
    val safeDays = days ?: return ""
    return when {
        safeDays <= 0L -> stringResource(R.string.study_interval_today)
        safeDays == 1L -> stringResource(R.string.study_interval_tomorrow)
        safeDays < DAYS_IN_WEEK -> stringResource(R.string.study_interval_days_format, safeDays.toInt())
        safeDays < DAYS_IN_MONTH -> stringResource(
            R.string.study_interval_weeks_format,
            (safeDays / DAYS_IN_WEEK).toInt(),
        )
        else -> stringResource(
            R.string.study_interval_months_format,
            (safeDays / DAYS_IN_MONTH).toInt(),
        )
    }
}

private val gradeChipMinHeight = 92.dp
private const val DAYS_IN_WEEK = 7L
private const val DAYS_IN_MONTH = 30L

@PreviewLightDark
@Composable
private fun StudyScreenPreview() {
    HelloTheme {
        StudyScreen(
            state = StudyUiState(
                isLoading = false,
                currentItem = StudySessionItem(
                    flashcardId = "1".toFlashcardId(),
                    review = FsrsCard.new("1".toFlashcardId(), SystemClock),
                    word = "Serendipity",
                    phonetic = "/ˌserənˈdɪpɪti/",
                    meaning = "The occurrence of events by chance in a happy way",
                    translation = "Casualidad afortunada",
                ),
                reviewedCount = 3,
                totalCount = 10,
            ),
        )
    }
}

@Composable
private fun GradeHintCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.study_grade_hint_title).uppercase(),
                    fontFamily = geistMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.12.em,
                    color = emberMuted,
                )
                Spacer(Modifier.height(4.dp))
                GradeHintRow(
                    label = stringResource(R.string.grade_again),
                    effect = stringResource(R.string.study_grade_hint_again_effect),
                    color = emberBad,
                )
                GradeHintRow(
                    label = stringResource(R.string.grade_hard),
                    effect = stringResource(R.string.study_grade_hint_hard_effect),
                    color = emberWarn,
                )
                GradeHintRow(
                    label = stringResource(R.string.grade_good),
                    effect = stringResource(R.string.study_grade_hint_good_effect),
                    color = emberOnBg,
                )
                GradeHintRow(
                    label = stringResource(R.string.grade_easy),
                    effect = stringResource(R.string.study_grade_hint_easy_effect),
                    color = emberAccent,
                )
            }
            Text(
                text = stringResource(R.string.study_grade_hint_dismiss),
                fontFamily = geist,
                fontWeight = FontWeight.Normal,
                fontSize = 13.5.sp,
                color = emberMuted,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(start = 12.dp, top = 2.dp),
            )
        }
    }
}

@Composable
private fun GradeHintRow(
    label: String,
    effect: String,
    color: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontFamily = geist,
            fontWeight = FontWeight.Medium,
            fontSize = 12.5.sp,
            color = color,
        )
        Text(
            text = "·",
            fontFamily = geist,
            fontWeight = FontWeight.Normal,
            fontSize = 12.5.sp,
            color = emberFaint,
        )
        Text(
            text = effect,
            fontFamily = geist,
            fontWeight = FontWeight.Normal,
            fontSize = 12.5.sp,
            color = emberMuted,
        )
    }
}

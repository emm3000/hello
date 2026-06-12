package com.emm.hello.newfeatures.study

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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.generation.EvaluationMode
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.StudyCardType
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import com.emm.domain.study.ReviewGrade
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
import com.emm.hello.core.theme.emberGood
import com.emm.hello.core.theme.emberHint
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberOnBg
import com.emm.hello.core.theme.emberSurface
import com.emm.hello.core.theme.emberWarn
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.instrumentSerif
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HBadgeGroup
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonSize
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HCard
import com.emm.hello.core.ui.HEmptyState
import kotlin.math.ceil

private const val CARD_TRANSITION_DURATION_MS = 220
private const val CARD_EXIT_FADE_DURATION_MS = 160
private const val CARD_ENTER_SCALE = 0.96f
private const val CARD_EXIT_SCALE = 0.92f
private const val MAX_RELATED_FORMS = 3
private val studyDockMinHeight = 220.dp
private val startHeaderSpacing = 14.dp
private val recallPromptFontSize = 48.sp
private val recallPromptLineHeight = 54.sp
private val answerInputBorderWidth = 1.5.dp
private val answerInputFontSize = 26.sp
private val answerInputLineHeight = 32.sp
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

private data class TypedAnswerState(
    val typedAnswer: String,
    val typedAnswerChecked: Boolean,
    val typedAnswerCorrect: Boolean,
)

private data class StartMeta(
    val totalCount: Int,
    val estimatedMinutes: Int,
)

private data class StudyDockCallbacks(
    val onStartSession: () -> Unit,
    val onRevealAnswer: () -> Unit,
    val onSkipTypedAnswer: () -> Unit,
    val onReviewAnswer: (ReviewGrade) -> Unit,
    val onTypedAnswerChange: (String) -> Unit,
    val onCheckTypedAnswer: () -> Unit,
    val onCreateCard: () -> Unit,
)

@Composable
fun StudyScreen(
    modifier: Modifier = Modifier,
    onBackRequested: () -> Unit = {},
    onFinishDialogDismissed: () -> Unit = {},
    onReviewAnswer: (StudySessionItem?, ReviewGrade) -> Unit = { _, _ -> },
    onCreateCard: () -> Unit = {},
    onGradeHintDismissed: () -> Unit = {},
    state: StudyUiState = StudyUiState(),
    showFinishDialog: Boolean = false,
) {
    val tts: TextToSpeechManager = rememberTextToSpeechManager()
    val isSpeaking by tts.isSpeaking.collectAsStateWithLifecycle()
    val ttsReady by tts.isReady.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    var cardFace by remember { mutableStateOf(CardFace.Front) }
    var typedAnswer by remember { mutableStateOf("") }
    var typedAnswerChecked by remember { mutableStateOf(false) }
    var typedAnswerCorrect by remember { mutableStateOf(false) }
    var sessionStarted by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    val progress = if (state.totalCount > 0) {
        state.reviewedCount.toFloat() / state.totalCount.toFloat()
    } else {
        0f
    }

    LaunchedEffect(state.currentItem?.studyCard?.cardId) {
        cardFace = CardFace.Front
        typedAnswer = ""
        typedAnswerChecked = false
        typedAnswerCorrect = false
    }

    val currentItem = state.currentItem
    val currentStudyCard = currentItem?.studyCard
    val needsTypedAnswer = currentStudyCard?.needsTypedAnswer == true
    val sessionStage = remember(
        sessionStarted,
        state.currentItem,
        state.totalCount,
        cardFace,
        typedAnswerChecked,
    ) {
        when {
            !sessionStarted && state.totalCount == 0 -> StudyStage.Empty
            !sessionStarted -> StudyStage.Start
            state.currentItem == null -> StudyStage.Empty
            cardFace == CardFace.Front -> StudyStage.Recall
            needsTypedAnswer && !typedAnswerChecked -> StudyStage.Check
            else -> StudyStage.Grade
        }
    }

    val gradePolicy = currentStudyCard?.gradePolicy(
        typedAnswerChecked = typedAnswerChecked,
        typedAnswerCorrect = typedAnswerCorrect,
    ) ?: ReviewGradePolicy()

    val estimatedMinutes = remember(state.totalCount) {
        maxOf(1, ceil(state.totalCount / 4f).toInt())
    }

    fun requestExit() {
        if (state.reviewedCount > 0 || sessionStarted) {
            showExitConfirmation = true
        } else {
            onBackRequested()
        }
    }

    val sessionInProgress = sessionStage == StudyStage.Recall ||
        sessionStage == StudyStage.Check ||
        sessionStage == StudyStage.Grade
    val stateLabel = when (sessionStage) {
        StudyStage.Recall -> stringResource(R.string.study_state_label_recall)
        StudyStage.Check -> stringResource(R.string.study_state_label_check)
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
                currentCount = state.reviewedCount,
                totalCount = state.totalCount,
                stateLabel = stateLabel,
                onClose = ::requestExit,
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
                        startMeta = StartMeta(
                            totalCount = state.totalCount,
                            estimatedMinutes = estimatedMinutes,
                        ),
                        cardViewState = CardViewState(
                            cardFace = cardFace,
                            progress = progress,
                        ),
                        typedAnswerState = TypedAnswerState(
                            typedAnswer = typedAnswer,
                            typedAnswerChecked = typedAnswerChecked,
                            typedAnswerCorrect = typedAnswerCorrect,
                        ),
                        audioState = AudioState(
                            isSpeaking = isSpeaking,
                            ttsReady = ttsReady,
                        ),
                        enabledGrades = gradePolicy.enabledGrades,
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
                        currentItem = currentItem,
                        intervalPreviews = state.intervalPreviews,
                        isGradeHintVisible = state.isGradeHintVisible,
                        typedAnswerState = TypedAnswerState(
                            typedAnswer = typedAnswer,
                            typedAnswerChecked = typedAnswerChecked,
                            typedAnswerCorrect = typedAnswerCorrect,
                        ),
                        callbacks = StudyDockCallbacks(
                            onStartSession = { sessionStarted = true },
                            onRevealAnswer = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                cardFace = CardFace.Back
                            },
                            onSkipTypedAnswer = {
                                typedAnswer = ""
                                typedAnswerChecked = true
                            },
                            onReviewAnswer = { grade ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReviewAnswer(state.currentItem, grade)
                            },
                            onTypedAnswerChange = {
                                typedAnswer = it
                                typedAnswerChecked = false
                            },
                            onCreateCard = onCreateCard,
                            onCheckTypedAnswer = {
                                currentStudyCard?.let { activeCard ->
                                    typedAnswerCorrect = matchesTypedAnswer(
                                        evaluationMode = activeCard.evaluationMode,
                                        typedAnswer = typedAnswer,
                                        expectedAnswer = activeCard.expectedAnswer,
                                        acceptedAnswers = activeCard.acceptedAnswers,
                                    )
                                    typedAnswerChecked = true
                                }
                            },
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

    if (showExitConfirmation) {
        HAlertDialog(
            title = stringResource(R.string.study_exit_confirm_title),
            description = stringResource(R.string.study_exit_confirm_desc),
            confirmText = stringResource(R.string.study_exit_confirm_leave),
            cancelText = stringResource(R.string.study_keep_going),
            isDangerous = true,
            onConfirm = {
                showExitConfirmation = false
                onBackRequested()
            },
            onDismiss = {
                showExitConfirmation = false
            },
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
    Start,
    Empty,
    Recall,
    Check,
    Grade,
}

@Composable
private fun StudyCanvas(
    sessionStage: StudyStage,
    currentItem: StudySessionItem?,
    startMeta: StartMeta,
    cardViewState: CardViewState,
    typedAnswerState: TypedAnswerState,
    audioState: AudioState,
    enabledGrades: Set<ReviewGrade>,
    onFlipCard: (CardFace) -> Unit,
    onGradeSwipe: (ReviewGrade) -> Unit,
    onStop: () -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (sessionStage) {
            StudyStage.Start -> StudyStartCard(
                totalCount = startMeta.totalCount,
                estimatedMinutes = startMeta.estimatedMinutes,
            )
            StudyStage.Empty -> StudyEmptyState()
            StudyStage.Recall,
            StudyStage.Check,
            StudyStage.Grade -> {
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
                            (
                                fadeIn(enterSpec) +
                                    scaleIn(initialScale = CARD_ENTER_SCALE, animationSpec = enterSpec)
                                )
                                .togetherWith(
                                    fadeOut(exitSpec) +
                                        scaleOut(targetScale = CARD_EXIT_SCALE, animationSpec = exitSpec)
                                )
                        },
                        label = "card_transition",
                        modifier = Modifier.fillMaxSize(),
                    ) { item ->
                        FlippableCard(
                            cardFace = cardViewState.cardFace,
                            onClick = onFlipCard,
                            progress = cardViewState.progress,
                            gradeEnabled = sessionStage == StudyStage.Grade,
                            enabledGrades = enabledGrades,
                            onGradeSwipe = onGradeSwipe,
                            modifier = Modifier.fillMaxSize(),
                            frontContent = {
                                FlashcardFrontContent(
                                    card = item?.flashcard,
                                    studyCard = item?.studyCard,
                                    prompt = item?.studyCard?.prompt ?: item?.flashcard?.word.orEmpty(),
                                    phonetic = if (item?.studyCard?.sourceField == "word") item.phonetic else "",
                                )
                            },
                            backContent = {
                                FlashcardBackContent(
                                    card = item?.flashcard,
                                    studyCard = item?.studyCard,
                                    prompt = item?.studyCard?.prompt
                                        ?: item?.flashcard?.word.orEmpty(),
                                    typedAnswer = typedAnswerState.typedAnswer,
                                    typedAnswerChecked = typedAnswerState.typedAnswerChecked,
                                    typedAnswerCorrect = typedAnswerState.typedAnswerCorrect,
                                )
                            },
                        )
                    }
                    currentItem?.studyCard?.cardType?.let { type ->
                        Text(
                            text = stringResource(type.directionLabelRes()),
                            fontFamily = geistMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.12.em,
                            color = emberMuted,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
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
        }
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
    IconButton(
        onClick = { if (isSpeaking) onStop() else onSpeak() },
        enabled = audioState.ttsReady,
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = description,
        )
    }
}

@Composable
private fun StudyActionDock(
    sessionStage: StudyStage,
    currentItem: StudySessionItem?,
    intervalPreviews: Map<ReviewGrade, Long>,
    isGradeHintVisible: Boolean,
    typedAnswerState: TypedAnswerState,
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
                StudyStage.Start -> {
                    HButton(
                        text = stringResource(R.string.study_start_cta_short),
                        onClick = callbacks.onStartSession,
                        variant = HButtonVariant.Accent,
                        size = HButtonSize.Lg,
                        full = true,
                    )
                    Text(
                        text = stringResource(R.string.study_start_supportive),
                        fontFamily = geistMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        letterSpacing = 0.08.em,
                        color = emberFaint,
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
                    val needsTyped = currentItem?.studyCard?.needsTypedAnswer == true
                    val recallCtaRes = if (needsTyped) {
                        R.string.study_recall_cta_type
                    } else {
                        R.string.study_recall_cta_reveal
                    }
                    HButton(
                        text = stringResource(recallCtaRes),
                        onClick = callbacks.onRevealAnswer,
                        variant = HButtonVariant.Accent,
                        size = HButtonSize.Lg,
                        full = true,
                    )
                }

                StudyStage.Check -> {
                    val studyCard = currentItem?.studyCard
                    val flashcard = currentItem?.flashcard
                    val placeholder = studyCard?.typedAnswerPlaceholder(flashcard)
                        ?: stringResource(R.string.study_typed_answer_placeholder_default)
                    val langIndicator = stringResource(
                        studyCard?.cardType.langIndicatorRes(),
                    )
                    StudyAnswerInput(
                        value = typedAnswerState.typedAnswer,
                        onValueChange = callbacks.onTypedAnswerChange,
                        placeholder = placeholder,
                        langIndicator = langIndicator,
                        onSubmit = callbacks.onCheckTypedAnswer,
                    )
                    HButton(
                        text = stringResource(R.string.study_check_answer),
                        onClick = callbacks.onCheckTypedAnswer,
                        variant = HButtonVariant.Accent,
                        size = HButtonSize.Lg,
                        full = true,
                        enabled = typedAnswerState.typedAnswer.isNotBlank(),
                    )
                    GhostUnderlineButton(
                        text = stringResource(R.string.study_reveal_anyway),
                        onClick = callbacks.onSkipTypedAnswer,
                    )
                }

                StudyStage.Grade -> {
                    if (isGradeHintVisible) {
                        GradeHintCard(onDismiss = onGradeHintDismissed)
                    }
                    val needsTypedAnswer = currentItem?.studyCard?.needsTypedAnswer == true
                    val gradePolicy = currentItem?.studyCard?.gradePolicy(
                        typedAnswerChecked = typedAnswerState.typedAnswerChecked,
                        typedAnswerCorrect = typedAnswerState.typedAnswerCorrect,
                    ) ?: ReviewGradePolicy()
                    AnswerButtons(
                        enabledGrades = gradePolicy.enabledGrades,
                        intervalPreviews = intervalPreviews,
                        onReviewAnswer = callbacks.onReviewAnswer,
                    )
                    val isLocked = needsTypedAnswer && typedAnswerState.typedAnswerChecked
                    val footnoteRes = when {
                        !isLocked -> null
                        typedAnswerState.typedAnswerCorrect -> R.string.study_grade_lock_easy_footnote
                        else -> R.string.study_grade_lock_hard_footnote
                    }
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
                    if (footnoteRes != null) {
                        Text(
                            text = stringResource(footnoteRes),
                            fontFamily = geist,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            color = emberMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyStartCard(totalCount: Int, estimatedMinutes: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(startHeaderSpacing),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.study_start_eyebrow),
                fontFamily = geistMono,
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                letterSpacing = 0.12.em,
                color = emberMuted,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.study_start_headline_count,
                    totalCount,
                    totalCount,
                ),
                fontFamily = instrumentSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 44.sp,
                lineHeight = 48.sp,
                letterSpacing = (-0.5).sp,
                color = emberOnBg,
            )
            Text(
                text = stringResource(R.string.study_start_stat_minutes, estimatedMinutes),
                fontFamily = geistMono,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                letterSpacing = 0.04.em,
                color = emberHint,
            )
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
    )
}

@Composable
private fun FlashcardFrontContent(
    card: Flashcard?,
    studyCard: GeneratedStudyCard?,
    prompt: String,
    phonetic: String,
) {
    val frontSupport = studyCard?.frontSupportText(card).orEmpty()
    var showSupport by remember(prompt) { mutableStateOf(false) }

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
                text = prompt,
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
            if (frontSupport.isNotBlank()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    ShowHintPill(
                        expanded = showSupport,
                        onClick = { showSupport = !showSupport },
                    )
                    if (showSupport) {
                        Text(
                            text = frontSupport,
                            fontFamily = instrumentSerif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            color = emberMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowHintPill(expanded: Boolean, onClick: () -> Unit) {
    val labelRes = if (expanded) R.string.study_recall_hide_hint else R.string.study_recall_show_hint
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = emberSurface,
        border = BorderStroke(1.dp, emberDivider),
    ) {
        Text(
            text = stringResource(labelRes),
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.08.em,
            color = emberMuted,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun FlashcardBackContent(
    card: Flashcard?,
    studyCard: GeneratedStudyCard?,
    prompt: String,
    typedAnswer: String,
    typedAnswerChecked: Boolean,
    typedAnswerCorrect: Boolean,
) {
    val needsTypedAnswer = studyCard?.needsTypedAnswer == true
    val shouldRevealAnswer = !needsTypedAnswer || typedAnswerChecked
    val isMismatch = needsTypedAnswer && typedAnswerChecked && !typedAnswerCorrect
    val primaryText = studyCard?.expectedAnswer ?: card?.translation.orEmpty()
    val resultMessage = studyCard?.typedAnswerResultMessage(typedAnswerCorrect).orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            !shouldRevealAnswer -> FlashcardBackPrompt(prompt = prompt)
            isMismatch -> FlashcardBackMismatch(
                typed = typedAnswer,
                expected = primaryText,
                resultMessage = resultMessage,
            )
            else -> FlashcardBackReveal(
                card = card,
                studyCard = studyCard,
                primaryText = primaryText,
                resultMessage = resultMessage,
                showMatchMessage = needsTypedAnswer && typedAnswerCorrect,
            )
        }
    }
}

@Composable
private fun FlashcardBackPrompt(prompt: String) {
    Text(
        text = prompt,
        fontFamily = instrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = recallPromptFontSize,
        lineHeight = recallPromptLineHeight,
        letterSpacing = (-0.5).sp,
        color = emberOnBg,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun FlashcardBackMismatch(
    typed: String,
    expected: String,
    resultMessage: String,
) {
    MismatchCards(typed = typed, expected = expected)
    if (resultMessage.isNotBlank()) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = resultMessage,
            fontFamily = instrumentSerif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = emberMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FlashcardBackReveal(
    card: Flashcard?,
    studyCard: GeneratedStudyCard?,
    primaryText: String,
    resultMessage: String,
    showMatchMessage: Boolean,
) {
    val answerLabel = studyCard?.gradeAnswerLabel()
        ?: stringResource(R.string.study_answer_label_default)
    val supportingText = studyCard?.supportingBackText(card).orEmpty()
    val backPhonetic = if (studyCard?.sourceField != "word" && card?.phonetic?.isNotBlank() == true) {
        card.phonetic
    } else {
        ""
    }

    Text(
        text = answerLabel,
        fontFamily = geistMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        letterSpacing = 0.12.em,
        color = emberMuted,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = primaryText,
        fontFamily = instrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = gradeAnswerFontSize,
        lineHeight = gradeAnswerLineHeight,
        letterSpacing = (-0.5).sp,
        textAlign = TextAlign.Center,
        color = emberOnBg,
    )

    if (backPhonetic.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = backPhonetic,
            fontFamily = geistMono,
            fontSize = 13.sp,
            color = emberHint,
            textAlign = TextAlign.Center,
        )
    }

    if (supportingText.isNotBlank()) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = supportingText,
            fontFamily = instrumentSerif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = emberMuted,
            textAlign = TextAlign.Center,
        )
    }

    if (showMatchMessage && resultMessage.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = resultMessage,
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.08.em,
            color = emberGood,
            textAlign = TextAlign.Center,
        )
    }

    studyCard?.let {
        CardTypeAnswerSupport(card = card, studyCard = it)
    }
}

@Composable
private fun MismatchCards(typed: String, expected: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MismatchRow(
            label = stringResource(R.string.study_grade_mismatch_you_wrote),
            value = typed.ifBlank { "—" },
            valueColor = emberBad,
            strikethrough = true,
        )
        MismatchRow(
            label = stringResource(R.string.study_grade_mismatch_expected),
            value = expected,
            valueColor = emberAccent,
            strikethrough = false,
        )
    }
}

@Composable
private fun MismatchRow(
    label: String,
    value: String,
    valueColor: Color,
    strikethrough: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(emberSurface, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 0.12.em,
            color = emberMuted,
        )
        Text(
            text = value,
            fontFamily = instrumentSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            color = valueColor,
            textDecoration = if (strikethrough) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}

@Composable
private fun CardTypeAnswerSupport(
    card: Flashcard?,
    studyCard: GeneratedStudyCard,
) {
    when (studyCard.cardType) {
        StudyCardType.Cloze -> {
            val context = card?.clozeSentence
                ?.takeIf(String::isNotBlank)
                ?: card?.sourceContext.orEmpty()
            if (context.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                HAlert(
                    title = stringResource(R.string.study_supporting_context_title),
                    description = context,
                    variant = AlertVariant.Default,
                )
            }
        }

        StudyCardType.Form -> {
            val forms = card?.irregularForms.orEmpty()
            if (forms.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.study_related_forms_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HBadgeGroup {
                        forms.take(MAX_RELATED_FORMS).forEach { form ->
                            HBadge(
                                label = form,
                                variant = BadgeVariant.Outline,
                            )
                        }
                    }
                }
            }
        }

        else -> Unit
    }
}

@Composable
private fun GeneratedStudyCard.frontSupportText(card: Flashcard?): String {
    return when (cardType) {
        StudyCardType.Recognition -> recognitionSupportText(card)
        StudyCardType.Production -> productionSupportText(card)
        StudyCardType.Cloze -> clozeSupportText(card)
        StudyCardType.Form -> formSupportText(card)
    }
}

private fun GeneratedStudyCard.recognitionSupportText(card: Flashcard?): String {
    return when {
        hint.isNotBlank() -> hint
        explanation.isNotBlank() -> explanation
        card?.meaning?.isNotBlank() == true -> card.meaning
        else -> ""
    }
}

private fun GeneratedStudyCard.productionSupportText(card: Flashcard?): String {
    return when {
        hint.isNotBlank() -> hint
        card?.sourceContext?.isNotBlank() == true -> card.sourceContext
        card?.whyUseful?.isNotBlank() == true -> card.whyUseful
        else -> ""
    }
}

private fun GeneratedStudyCard.clozeSupportText(card: Flashcard?): String {
    return when {
        card?.sourceContext?.isNotBlank() == true -> card.sourceContext
        hint.isNotBlank() -> hint
        explanation.isNotBlank() -> explanation
        else -> ""
    }
}

@Composable
private fun GeneratedStudyCard.formSupportText(card: Flashcard?): String {
    return when {
        card?.irregularForms?.isNotEmpty() == true -> {
            stringResource(R.string.study_related_forms_inline, card.irregularForms.joinToString())
        }
        card?.usagePattern?.isNotBlank() == true -> card.usagePattern
        hint.isNotBlank() -> hint
        else -> ""
    }
}

@Composable
private fun GeneratedStudyCard.gradeAnswerLabel(): String {
    return stringResource(
        when (cardType) {
            StudyCardType.Recognition -> R.string.study_grade_answer_label_recognition
            StudyCardType.Production -> R.string.study_grade_answer_label_production
            StudyCardType.Cloze -> R.string.study_grade_answer_label_cloze
            StudyCardType.Form -> R.string.study_grade_answer_label_form
        }
    )
}

private fun GeneratedStudyCard.supportingBackText(card: Flashcard?): String {
    return when {
        explanation.isNotBlank() -> explanation
        hint.isNotBlank() -> hint
        cardType == StudyCardType.Cloze && card?.meaning?.isNotBlank() == true -> card.meaning
        cardType == StudyCardType.Form && card?.usagePattern?.isNotBlank() == true -> card.usagePattern
        card?.whyUseful?.isNotBlank() == true -> card.whyUseful
        card?.meaning?.isNotBlank() == true -> card.meaning
        else -> ""
    }
}

@Composable
private fun GeneratedStudyCard.typedAnswerPlaceholder(card: Flashcard?): String {
    return when (cardType) {
        StudyCardType.Recognition -> stringResource(R.string.study_typed_answer_placeholder_recognition)
        StudyCardType.Production -> stringResource(R.string.study_typed_answer_placeholder_production)
        StudyCardType.Cloze -> stringResource(R.string.study_typed_answer_placeholder_cloze)
        StudyCardType.Form -> stringResource(
            if (card?.irregularForms?.isNotEmpty() == true) {
                R.string.study_typed_answer_placeholder_form_specific
            } else {
                R.string.study_typed_answer_placeholder_form_generic
            }
        )
    }
}

@Composable
private fun GeneratedStudyCard.typedAnswerResultMessage(isCorrect: Boolean): String {
    if (isCorrect) {
        return when (evaluationMode) {
            EvaluationMode.Exact -> stringResource(R.string.study_typed_answer_exact_match)
            EvaluationMode.FlexibleText -> stringResource(R.string.study_typed_answer_flexible_match)
            EvaluationMode.ManualSelfCheck -> ""
        }
    }
    return when (evaluationMode) {
        EvaluationMode.Exact -> stringResource(R.string.study_typed_answer_no_exact_match)
        EvaluationMode.FlexibleText -> stringResource(R.string.study_typed_answer_no_flexible_match)
        EvaluationMode.ManualSelfCheck -> ""
    }
}

@Composable
private fun AnswerButtons(
    modifier: Modifier = Modifier,
    enabledGrades: Set<ReviewGrade> = ReviewGrade.entries.toSet(),
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
                enabled = ReviewGrade.AGAIN in enabledGrades,
                onClick = { onReviewAnswer(ReviewGrade.AGAIN) },
                modifier = Modifier.weight(1f),
            )
            GradeChip(
                grade = ReviewGrade.HARD,
                intervalDays = intervalPreviews[ReviewGrade.HARD],
                enabled = ReviewGrade.HARD in enabledGrades,
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
                enabled = ReviewGrade.GOOD in enabledGrades,
                onClick = { onReviewAnswer(ReviewGrade.GOOD) },
                modifier = Modifier.weight(1f),
            )
            GradeChip(
                grade = ReviewGrade.EASY,
                intervalDays = intervalPreviews[ReviewGrade.EASY],
                enabled = ReviewGrade.EASY in enabledGrades,
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
                currentItem = StudySessionItem(
                    flashcardId = "1".toFlashcardId(),
                    review = FlashcardReview.empty(SystemClock),
                    studyCard = GeneratedStudyCard(
                        cardId = "study-card-1",
                        cardType = StudyCardType.Recognition,
                        prompt = "Serendipity",
                        expectedAnswer = "Casualidad afortunada",
                        evaluationMode = EvaluationMode.ManualSelfCheck,
                    ),
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

private fun StudyCardType.directionLabelRes(): Int = when (this) {
    StudyCardType.Recognition -> R.string.study_direction_en_to_es
    StudyCardType.Production -> R.string.study_direction_es_to_en
    StudyCardType.Cloze -> R.string.study_direction_cloze
    StudyCardType.Form -> R.string.study_direction_form
}

private fun StudyCardType?.langIndicatorRes(): Int = when (this) {
    StudyCardType.Recognition -> R.string.study_check_lang_indicator_es
    StudyCardType.Production -> R.string.study_check_lang_indicator_en
    StudyCardType.Cloze -> R.string.study_check_lang_indicator_en
    StudyCardType.Form -> R.string.study_check_lang_indicator_en
    null -> R.string.study_check_lang_indicator_en
}

@Composable
private fun StudyAnswerInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    langIndicator: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(emberSurface, RoundedCornerShape(16.dp))
            .border(answerInputBorderWidth, emberAccent, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontFamily = instrumentSerif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontSize = answerInputFontSize,
                    lineHeight = answerInputLineHeight,
                    color = emberMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = instrumentSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = answerInputFontSize,
                    lineHeight = answerInputLineHeight,
                    color = emberOnBg,
                ),
                cursorBrush = SolidColor(emberAccent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = langIndicator,
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.12.em,
            color = emberFaint,
        )
    }
}

@Composable
private fun GhostUnderlineButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontFamily = geist,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        color = emberMuted,
        textDecoration = TextDecoration.Underline,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
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

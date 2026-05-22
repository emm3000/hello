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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberFaint
import com.emm.hello.core.theme.emberHint
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberOnBg
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.instrumentSerif
import com.emm.hello.core.theme.semanticColors
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HBadgeGroup
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonSize
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HEmptyState
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HSeparator
import kotlin.math.ceil

private const val CARD_TRANSITION_DURATION_MS = 220
private const val CARD_EXIT_FADE_DURATION_MS = 160
private const val CARD_ENTER_SCALE = 0.96f
private const val CARD_EXIT_SCALE = 0.92f
private const val MEANING_SEPARATOR_WIDTH_FRACTION = 0.5f
private const val MAX_RELATED_FORMS = 3
private val studyDockMinHeight = 220.dp
private val startHeaderSpacing = 14.dp

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
)

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
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                                    typedAnswerChecked = typedAnswerState.typedAnswerChecked,
                                    typedAnswerCorrect = typedAnswerState.typedAnswerCorrect,
                                )
                            },
                        )
                    }
                    currentItem?.studyCard?.cardType?.let { type ->
                        HBadge(
                            label = type.label,
                            variant = BadgeVariant.Outline,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp),
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
    typedAnswerState: TypedAnswerState,
    callbacks: StudyDockCallbacks,
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

                StudyStage.Empty -> Unit

                StudyStage.Recall -> {
                    if (currentItem?.studyCard?.needsTypedAnswer == true) {
                        HButton(
                            text = stringResource(R.string.study_answer_cta),
                            onClick = callbacks.onRevealAnswer,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        TapToRevealHint(modifier = Modifier.fillMaxWidth())
                    }
                }

                StudyStage.Check -> {
                    val studyCard = currentItem?.studyCard
                    val flashcard = currentItem?.flashcard
                    Text(
                        text = stringResource(R.string.study_answer_guidance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val typedLabel = studyCard?.typedAnswerLabel()
                        ?: stringResource(R.string.study_typed_answer_label_default)
                    val typedPlaceholder = studyCard?.typedAnswerPlaceholder(flashcard)
                        ?: stringResource(R.string.study_typed_answer_placeholder_default)
                    HInput(
                        value = typedAnswerState.typedAnswer,
                        onValueChange = callbacks.onTypedAnswerChange,
                        label = typedLabel,
                        placeholder = typedPlaceholder,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { callbacks.onCheckTypedAnswer() }),
                    )
                    HButton(
                        text = stringResource(R.string.study_check_answer),
                        onClick = callbacks.onCheckTypedAnswer,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = typedAnswerState.typedAnswer.isNotBlank(),
                    )
                    HButton(
                        text = stringResource(R.string.study_reveal_anyway),
                        onClick = callbacks.onSkipTypedAnswer,
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Ghost,
                    )
                }

                StudyStage.Grade -> {
                    val needsTypedAnswer = currentItem?.studyCard?.needsTypedAnswer == true
                    val gradePolicy = currentItem?.studyCard?.gradePolicy(
                        typedAnswerChecked = typedAnswerState.typedAnswerChecked,
                        typedAnswerCorrect = typedAnswerState.typedAnswerCorrect,
                    ) ?: ReviewGradePolicy()
                    if (
                        needsTypedAnswer &&
                        typedAnswerState.typedAnswer.isBlank() &&
                        typedAnswerState.typedAnswerChecked
                    ) {
                        Text(
                            text = stringResource(R.string.study_skip_guidance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AnswerButtons(
                        enabledGrades = gradePolicy.enabledGrades,
                        guidance = gradePolicy.guidance,
                        intervalPreviews = intervalPreviews,
                        onReviewAnswer = callbacks.onReviewAnswer,
                    )
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
    HEmptyState(
        modifier = Modifier.fillMaxSize(),
        headline = stringResource(R.string.study_empty_headline),
        body = stringResource(R.string.study_empty_body),
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
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 56.dp),
        ) {
            CardTypePromptBlock(
                card = card,
                studyCard = studyCard,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = prompt,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (phonetic.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = phonetic,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (frontSupport.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                IconButton(
                    onClick = { showSupport = !showSupport },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.study_show_hint_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (showSupport) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Text(
                            text = frontSupport,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardBackContent(
    card: Flashcard?,
    studyCard: GeneratedStudyCard?,
    typedAnswerChecked: Boolean,
    typedAnswerCorrect: Boolean,
) {
    val needsTypedAnswer = studyCard?.needsTypedAnswer == true
    val shouldRevealAnswer = !needsTypedAnswer || typedAnswerChecked
    val primaryText = studyCard?.expectedAnswer ?: card?.translation.orEmpty()
    val answerLabel = studyCard?.answerLabel()
        ?: stringResource(R.string.study_answer_label_default)
    val resultMessage = studyCard?.typedAnswerResultMessage(typedAnswerCorrect).orEmpty()
    val supportingText = studyCard?.supportingBackText(card).orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (shouldRevealAnswer) {
            Text(
                text = answerLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = primaryText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (needsTypedAnswer) {
                Spacer(Modifier.height(8.dp))
                TypedAnswerResultRow(
                    isCorrect = typedAnswerCorrect,
                    message = resultMessage,
                )
            }

            if (supportingText.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                HSeparator(modifier = Modifier.fillMaxWidth(MEANING_SEPARATOR_WIDTH_FRACTION))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            studyCard?.let {
                CardTypeAnswerSupport(
                    card = card,
                    studyCard = it,
                )
            }
        }
    }
}

@Composable
private fun TypedAnswerResultRow(
    isCorrect: Boolean,
    message: String,
) {
    val tint = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val icon = if (isCorrect) Icons.Outlined.Check else Icons.Outlined.Close
    val iconDescription = stringResource(
        if (isCorrect) R.string.study_correct_icon_desc else R.string.study_incorrect_icon_desc
    )
    val prefixRes = if (isCorrect) {
        R.string.study_typed_answer_correct_prefix
    } else {
        R.string.study_typed_answer_incorrect_prefix
    }
    val prefix = stringResource(prefixRes)
    val fullMessage = if (message.isBlank()) prefix else "$prefix · $message"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconDescription,
            tint = tint,
            modifier = Modifier.height(18.dp),
        )
        Text(
            text = fullMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = tint,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CardTypePromptBlock(
    card: Flashcard?,
    studyCard: GeneratedStudyCard?,
) {
    when (studyCard?.cardType) {
        StudyCardType.Cloze -> {
            Text(
                text = stringResource(R.string.study_cloze_prompt_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        StudyCardType.Form -> {
            val formHints = buildList {
                addAll(card?.irregularForms.orEmpty())
                if (card?.usagePattern?.isNotBlank() == true) {
                    add(card.usagePattern)
                }
            }.distinct()

            if (formHints.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.study_form_hints_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HBadgeGroup {
                        formHints.take(2).forEach { hint ->
                            HBadge(
                                label = hint,
                                variant = BadgeVariant.Secondary,
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
private fun GeneratedStudyCard.answerLabel(): String {
    return stringResource(
        when (cardType) {
            StudyCardType.Recognition -> R.string.study_answer_label_recognition
            StudyCardType.Production -> R.string.study_answer_label_production
            StudyCardType.Cloze -> R.string.study_answer_label_cloze
            StudyCardType.Form -> R.string.study_answer_label_form
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
private fun GeneratedStudyCard.typedAnswerLabel(): String {
    return stringResource(
        when (cardType) {
            StudyCardType.Recognition -> R.string.study_typed_answer_label_recognition
            StudyCardType.Production -> R.string.study_typed_answer_label_production
            StudyCardType.Cloze -> R.string.study_typed_answer_label_cloze
            StudyCardType.Form -> R.string.study_typed_answer_label_form
        }
    )
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
private fun TapToRevealHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.tap_to_reveal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AnswerButtons(
    modifier: Modifier = Modifier,
    enabledGrades: Set<ReviewGrade> = ReviewGrade.entries.toSet(),
    guidance: String = "",
    intervalPreviews: Map<ReviewGrade, Long> = emptyMap(),
    onReviewAnswer: (ReviewGrade) -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (guidance.isNotBlank()) {
            Text(
                text = guidance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
    val tokens = gradeChipTokens(grade)
    val alpha = if (enabled) 1f else GRADE_CHIP_DISABLED_ALPHA
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = gradeChipMinHeight),
        shape = MaterialTheme.shapes.medium,
        color = tokens.container.copy(alpha = alpha),
        contentColor = tokens.content,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = tokens.icon,
                contentDescription = null,
                tint = tokens.content,
                modifier = Modifier
                    .align(Alignment.Start)
                    .size(18.dp),
            )
            Spacer(Modifier.weight(1f, fill = false))
            Text(
                text = stringResource(tokens.labelRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = tokens.content,
            )
            Text(
                text = formatInterval(intervalDays),
                style = MaterialTheme.typography.labelSmall,
                color = tokens.content.copy(alpha = GRADE_CHIP_INTERVAL_ALPHA),
            )
        }
    }
}

private data class GradeChipTokens(
    val container: Color,
    val content: Color,
    val icon: ImageVector,
    val labelRes: Int,
)

@Composable
private fun gradeChipTokens(grade: ReviewGrade): GradeChipTokens {
    val cs = MaterialTheme.colorScheme
    val warning = MaterialTheme.semanticColors.warning
    return when (grade) {
        ReviewGrade.AGAIN -> GradeChipTokens(
            container = cs.errorContainer,
            content = cs.onErrorContainer,
            icon = Icons.Outlined.Refresh,
            labelRes = R.string.grade_again,
        )
        ReviewGrade.HARD -> GradeChipTokens(
            container = warning.container,
            content = warning.content,
            icon = Icons.Outlined.Warning,
            labelRes = R.string.grade_hard,
        )
        ReviewGrade.GOOD -> GradeChipTokens(
            container = cs.primaryContainer,
            content = cs.onPrimaryContainer,
            icon = Icons.Rounded.CheckCircle,
            labelRes = R.string.grade_good,
        )
        ReviewGrade.EASY -> GradeChipTokens(
            container = cs.secondaryContainer,
            content = cs.onSecondaryContainer,
            icon = Icons.Rounded.Bolt,
            labelRes = R.string.grade_easy,
        )
    }
}

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

private const val GRADE_CHIP_DISABLED_ALPHA = 0.4f
private const val GRADE_CHIP_INTERVAL_ALPHA = 0.75f
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

private val StudyCardType.label: String
    get() = when (this) {
        StudyCardType.Recognition -> "Recognition"
        StudyCardType.Production -> "Production"
        StudyCardType.Cloze -> "Cloze"
        StudyCardType.Form -> "Form"
    }

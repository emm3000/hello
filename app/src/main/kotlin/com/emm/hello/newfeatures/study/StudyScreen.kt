package com.emm.hello.newfeatures.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
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
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HBadgeGroup
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HProgressBar
import com.emm.hello.core.ui.HSeparator
import kotlin.math.ceil

private const val CARD_TRANSITION_DURATION_MS = 350
private const val CARD_TRANSITION_DIVISOR = 3
private const val CARD_EXIT_FADE_DURATION_MS = 250
private const val PHONETIC_SEPARATOR_WIDTH_FRACTION = 0.4f
private const val MEANING_SEPARATOR_WIDTH_FRACTION = 0.5f
private const val SUPPORT_SEPARATOR_WIDTH_FRACTION = 0.7f
private const val MAX_RELATED_FORMS = 3
private val studyDockMinHeight = 220.dp

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

    val prevStudyItem = remember { mutableStateOf(state.currentItem) }
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.study_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(
                                R.string.study_progress_of,
                                state.reviewedCount,
                                state.totalCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = ::requestExit) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StudyStageHeader(sessionStage = sessionStage)

                StudyCanvas(
                    sessionStage = sessionStage,
                    currentItem = currentItem,
                    totalCount = state.totalCount,
                    prevStudyItem = prevStudyItem.value,
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
                    onFlipCard = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        cardFace = it.next
                    },
                    onCardAnimationFinished = { prevStudyItem.value = state.currentItem },
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
                    estimatedMinutes = estimatedMinutes,
                    totalCount = state.totalCount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                )
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
private fun StudyStageHeader(sessionStage: StudyStage) {
    val message = when (sessionStage) {
        StudyStage.Start -> null
        StudyStage.Empty -> null
        StudyStage.Recall -> stringResource(R.string.study_prompt_guidance)
        StudyStage.Check -> stringResource(R.string.study_answer_guidance)
        StudyStage.Grade -> null
    }

    if (message == null) {
        Spacer(Modifier.height(8.dp))
    } else {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StudyCanvas(
    sessionStage: StudyStage,
    currentItem: StudySessionItem?,
    totalCount: Int,
    prevStudyItem: StudySessionItem?,
    cardViewState: CardViewState,
    typedAnswerState: TypedAnswerState,
    audioState: AudioState,
    onFlipCard: (CardFace) -> Unit,
    onCardAnimationFinished: (Float) -> Unit,
    onStop: () -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        when (sessionStage) {
            StudyStage.Start -> StudyStartCard(
                totalCount = totalCount,
            )
            StudyStage.Empty -> StudyEmptyState()
            StudyStage.Recall,
            StudyStage.Check,
            StudyStage.Grade -> {
                AnimatedContent(
                    targetState = currentItem,
                    transitionSpec = {
                        (
                            slideInHorizontally(tween(CARD_TRANSITION_DURATION_MS)) {
                                it / CARD_TRANSITION_DIVISOR
                            } + fadeIn(tween(CARD_TRANSITION_DURATION_MS))
                            )
                            .togetherWith(
                                slideOutHorizontally(tween(CARD_TRANSITION_DURATION_MS)) {
                                    -it / CARD_TRANSITION_DIVISOR
                                } + fadeOut(tween(CARD_EXIT_FADE_DURATION_MS))
                            )
                    },
                    label = "card_transition",
                    modifier = Modifier.fillMaxSize(),
                ) { item ->
                    FlippableCard(
                        cardFace = cardViewState.cardFace,
                        onClick = onFlipCard,
                        progress = cardViewState.progress,
                        onFinished = onCardAnimationFinished,
                        modifier = Modifier.fillMaxSize(),
                        frontContent = {
                            FlashcardFrontContent(
                                card = item?.flashcard,
                                studyCard = item?.studyCard,
                                prompt = item?.studyCard?.prompt ?: item?.flashcard?.word.orEmpty(),
                                phonetic = if (item?.studyCard?.sourceField == "word") item.phonetic else "",
                                cardType = item?.studyCard?.cardType,
                            )
                        },
                        backContent = {
                            FlashcardBackContent(
                                card = prevStudyItem?.flashcard,
                                studyCard = prevStudyItem?.studyCard,
                                typedAnswerChecked = typedAnswerState.typedAnswerChecked,
                                typedAnswerCorrect = typedAnswerState.typedAnswerCorrect,
                                isSpeaking = audioState.isSpeaking,
                                ttsReady = audioState.ttsReady,
                                onStop = onStop,
                                onSpeak = onSpeak,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyActionDock(
    sessionStage: StudyStage,
    currentItem: StudySessionItem?,
    typedAnswerState: TypedAnswerState,
    callbacks: StudyDockCallbacks,
    estimatedMinutes: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        AnimatedContent(
            targetState = sessionStage,
            transitionSpec = {
                fadeIn(tween(CARD_TRANSITION_DURATION_MS)) togetherWith
                    fadeOut(tween(CARD_EXIT_FADE_DURATION_MS)) using
                    SizeTransform(clip = false)
            },
            label = "study_action_dock",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = studyDockMinHeight),
        ) { stage ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (stage) {
                    StudyStage.Start -> {
                        Text(
                            text = stringResource(R.string.study_start_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(
                                R.string.study_start_desc,
                                totalCount,
                                estimatedMinutes,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HButton(
                            text = stringResource(R.string.study_start_cta),
                            onClick = callbacks.onStartSession,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    StudyStage.Empty -> {
                        Text(
                            text = stringResource(R.string.study_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.study_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    StudyStage.Recall -> {
                        Text(
                            text = stringResource(R.string.tap_to_reveal),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HButton(
                            text = if (currentItem?.studyCard?.needsTypedAnswer == true) {
                                stringResource(R.string.study_answer_cta)
                            } else {
                                stringResource(R.string.study_reveal_answer)
                            },
                            onClick = callbacks.onRevealAnswer,
                            modifier = Modifier.fillMaxWidth(),
                        )
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
                            onReviewAnswer = callbacks.onReviewAnswer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyStartCard(totalCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HBadge(
                label = totalCount.toString(),
                variant = BadgeVariant.Secondary,
            )
            Text(
                text = stringResource(R.string.study_start_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.study_prompt_guidance),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StudyEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.study_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.study_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FlashcardFrontContent(
    card: Flashcard?,
    studyCard: GeneratedStudyCard?,
    prompt: String,
    phonetic: String,
    cardType: StudyCardType?,
) {
    val frontTitle = studyCard?.frontTitle().orEmpty()
    val frontSupport = studyCard?.frontSupportText(card).orEmpty()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            CardTypePromptBlock(
                card = card,
                studyCard = studyCard,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = prompt,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (frontTitle.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = frontTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
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
            if (frontSupport.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                HSeparator(modifier = Modifier.fillMaxWidth(SUPPORT_SEPARATOR_WIDTH_FRACTION))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = frontSupport,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
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
    isSpeaking: Boolean,
    ttsReady: Boolean,
    onStop: () -> Unit = {},
    onSpeak: () -> Unit = {},
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

        Spacer(Modifier.height(20.dp))

        HButton(
            text = if (isSpeaking) stringResource(R.string.stop_speech_desc) else stringResource(R.string.speak_desc),
            onClick = { if (isSpeaking) onStop() else onSpeak() },
            enabled = ttsReady,
            variant = ButtonVariant.Ghost,
            leadingIcon = Icons.AutoMirrored.Filled.VolumeUp,
        )
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
private fun GeneratedStudyCard.frontTitle(): String {
    return stringResource(
        when (cardType) {
            StudyCardType.Recognition -> R.string.study_front_title_recognition
            StudyCardType.Production -> R.string.study_front_title_production
            StudyCardType.Cloze -> R.string.study_front_title_cloze
            StudyCardType.Form -> R.string.study_front_title_form
        }
    )
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
private fun AnswerButtons(
    modifier: Modifier = Modifier,
    enabledGrades: Set<ReviewGrade> = ReviewGrade.entries.toSet(),
    guidance: String = "",
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
            HButton(
                text = stringResource(R.string.grade_again),
                onClick = { onReviewAnswer(ReviewGrade.AGAIN) },
                variant = ButtonVariant.Destructive,
                enabled = ReviewGrade.AGAIN in enabledGrades,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Outlined.Refresh,
            )
            HButton(
                text = stringResource(R.string.grade_hard),
                onClick = { onReviewAnswer(ReviewGrade.HARD) },
                variant = ButtonVariant.Secondary,
                enabled = ReviewGrade.HARD in enabledGrades,
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
                enabled = ReviewGrade.GOOD in enabledGrades,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Rounded.CheckCircle,
            )
            HButton(
                text = stringResource(R.string.grade_easy),
                onClick = { onReviewAnswer(ReviewGrade.EASY) },
                variant = ButtonVariant.Outline,
                enabled = ReviewGrade.EASY in enabledGrades,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Rounded.Bolt,
            )
        }
    }
}

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

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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.hairline
import com.emm.hello.core.theme.surface
import com.emm.hello.core.theme.inkFaint
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.surfaceRaised
import com.emm.hello.core.theme.schibsted
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant
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
        color = pageBackground,
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
                color = pageBackground,
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
            color = surface,
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
                    fontFamily = schibsted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.12.em,
                    color = inkMuted,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.session_completed_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 44.sp,
                    lineHeight = (44 * 1.04f).sp,
                    letterSpacing = (-0.02).em,
                    color = ink,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.session_completed_desc, totalCount),
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp,
                    color = inkMuted,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surface, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = totalCount.toString(),
                        fontFamily = schibsted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        color = ink,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.session_completed_stat_label).uppercase(),
                        fontFamily = schibsted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 0.12.em,
                        color = inkMuted,
                    )
                }
                Spacer(Modifier.height(24.dp))
                HButton(
                    text = stringResource(R.string.session_completed_cta),
                    onClick = onDismiss,
                    variant = HButtonVariant.Primary,
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
            fontFamily = schibsted,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 0.12.em,
            color = inkMuted,
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
        tint = ink,
        enabled = audioState.ttsReady,
    )
}

@Composable
private fun StudyActionDock(
    sessionStage: StudyStage,
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
                StudyStage.Loading -> Unit

                StudyStage.Error -> {
                    HButton(
                        text = stringResource(R.string.study_error_retry),
                        onClick = callbacks.onRetryLoad,
                        variant = HButtonVariant.Primary,
                        full = true,
                    )
                }

                StudyStage.Empty -> {
                    HButton(
                        text = stringResource(R.string.study_empty_create_card_cta),
                        onClick = callbacks.onCreateCard,
                        variant = HButtonVariant.Secondary,
                        full = true,
                    )
                }

                StudyStage.Recall -> {
                    HButton(
                        text = stringResource(R.string.study_recall_cta_reveal),
                        onClick = callbacks.onRevealAnswer,
                        variant = HButtonVariant.Primary,
                        full = true,
                    )
                }

                StudyStage.Grade -> {
                    AnswerButtons(
                        onReviewAnswer = callbacks.onReviewAnswer,
                    )
                    Text(
                        text = stringResource(R.string.study_grade_swipe_hint),
                        fontFamily = schibsted,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.12.em,
                        color = inkFaint,
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
    )
}

@Composable
private fun StudyLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        HLoadingSpinner(color = ink)
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
                fontWeight = FontWeight.SemiBold,
                fontSize = recallPromptFontSize,
                lineHeight = recallPromptLineHeight,
                letterSpacing = (-0.02).em,
                color = ink,
                textAlign = TextAlign.Center,
            )
            if (phonetic.isNotBlank()) {
                Text(
                    text = phonetic,
                    fontFamily = schibsted,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = inkMuted,
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = item.translation,
            fontWeight = FontWeight.SemiBold,
            fontSize = gradeAnswerFontSize,
            lineHeight = gradeAnswerLineHeight,
            letterSpacing = (-0.02).em,
            textAlign = TextAlign.Center,
            color = ink,
        )
        if (item.example.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = highlightWordInExample(item.example, item.word),
                fontFamily = schibsted,
                fontWeight = FontWeight.Normal,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                color = ink,
                textAlign = TextAlign.Center,
            )
            if (item.exampleTranslation.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.exampleTranslation,
                    fontFamily = schibsted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = inkMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        val referenceLine: String = listOf(item.phonetic, item.partOfSpeech)
            .filter(String::isNotBlank)
            .joinToString(" · ")
        if (referenceLine.isNotBlank()) {
            Text(
                text = referenceLine,
                fontFamily = schibsted,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                letterSpacing = 0.08.em,
                color = inkMuted,
                textAlign = TextAlign.Center,
            )
        }
        if (item.meaning.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = item.meaning,
                fontFamily = schibsted,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = inkMuted,
                textAlign = TextAlign.Center,
            )
        }
        if (item.irregularForms.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.study_related_forms_inline, item.irregularForms.joinToString()),
                fontFamily = schibsted,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                letterSpacing = 0.08.em,
                color = inkMuted,
                textAlign = TextAlign.Center,
            )
        }
        if (item.usagePattern.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = item.usagePattern,
                fontFamily = schibsted,
                fontWeight = FontWeight.Normal,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                color = inkMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun highlightWordInExample(example: String, word: String): AnnotatedString {
    val matchStart: Int = if (word.isBlank()) -1 else example.indexOf(word, ignoreCase = true)
    return buildAnnotatedString {
        if (matchStart < 0) {
            append(example)
            return@buildAnnotatedString
        }
        val matchEnd: Int = matchStart + word.length
        append(example.substring(0, matchStart))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(example.substring(matchStart, matchEnd))
        }
        append(example.substring(matchEnd))
    }
}

@Composable
private fun AnswerButtons(
    modifier: Modifier = Modifier,
    onReviewAnswer: (ReviewGrade) -> Unit = {},
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GradeForgotButton(
            onClick = { onReviewAnswer(ReviewGrade.AGAIN) },
            modifier = Modifier.weight(1f),
        )
        GradeKnewButton(
            onClick = { onReviewAnswer(ReviewGrade.GOOD) },
            onLongClick = { onReviewAnswer(ReviewGrade.EASY) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GradeForgotButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.helloShapes.control
    Box(
        modifier = modifier
            .heightIn(min = gradeChipMinHeight)
            .clip(shape)
            .background(Color.Transparent, shape)
            .border(1.dp, hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.study_grade_forgot),
            fontFamily = schibsted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = ink,
        )
    }
}

@Composable
private fun GradeKnewButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.helloShapes.control
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .heightIn(min = gradeChipMinHeight)
            .clip(shape)
            .background(surfaceRaised, shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.study_grade_knew),
            fontFamily = schibsted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = ink,
        )
    }
}

private val gradeChipMinHeight = 92.dp

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

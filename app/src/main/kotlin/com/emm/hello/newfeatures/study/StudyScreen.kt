package com.emm.hello.newfeatures.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
import com.emm.hello.core.theme.bricolage
import com.emm.hello.core.theme.cardHues
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.inkSoft
import com.emm.hello.core.theme.onInk
import com.emm.hello.core.theme.outline
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.schibsted
import com.emm.hello.core.theme.surface
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HEmptyState
import com.emm.hello.core.ui.HIconButton
import com.emm.hello.core.ui.HLoadingSpinner

private const val CARD_TRANSITION_DURATION_MS = 220
private const val CARD_EXIT_FADE_DURATION_MS = 160
private const val CARD_ENTER_SCALE = 0.96f
private const val CARD_EXIT_SCALE = 0.92f
private val gradeButtonMinHeight = 56.dp

private data class AudioState(
    val isSpeaking: Boolean,
    val ttsReady: Boolean,
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
    BackHandler(onBack = onExit)
    val tts: TextToSpeechManager = rememberTextToSpeechManager()
    val isSpeaking: Boolean by tts.isSpeaking.collectAsStateWithLifecycle()
    val ttsReady: Boolean by tts.isReady.collectAsStateWithLifecycle()
    val haptics: HapticFeedback = LocalHapticFeedback.current

    var cardFace by remember { mutableStateOf(CardFace.Front) }

    LaunchedEffect(state.currentItem?.flashcardId) {
        cardFace = CardFace.Front
    }

    val currentItem: StudySessionItem? = state.currentItem
    val sessionStage: StudyStage = remember(state.isLoading, state.loadError, state.currentItem, cardFace) {
        when {
            state.isLoading -> StudyStage.Loading
            state.loadError != null -> StudyStage.Error
            state.currentItem == null -> StudyStage.Empty
            cardFace == CardFace.Front -> StudyStage.Recall
            else -> StudyStage.Grade
        }
    }

    val onCard: Boolean = sessionStage == StudyStage.Recall || sessionStage == StudyStage.Grade
    val hueIndex: Int = if (state.totalCount > 0) {
        minOf(state.reviewedCount, state.totalCount - 1) % cardHues.size
    } else {
        0
    }
    val targetBackground: Color = if (onCard) cardHues[hueIndex] else pageBackground
    val background: Color by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(durationMillis = CARD_TRANSITION_DURATION_MS),
        label = "study_background",
    )

    val cardPosition: Int = minOf(state.reviewedCount + 1, state.totalCount)
    val position: String? = if (onCard && state.totalCount > 0) {
        stringResource(R.string.study_position, cardPosition, state.totalCount)
    } else {
        null
    }
    val progress: Float? = if (onCard && state.totalCount > 0) {
        cardPosition.toFloat() / state.totalCount.toFloat()
    } else {
        null
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            StudyTop(
                position = position,
                progress = progress,
                onClose = onExit,
                actions = if (onCard) {
                    {
                        TtsFloatingButton(
                            audioState = AudioState(isSpeaking = isSpeaking, ttsReady = ttsReady),
                            onSpeak = {
                                if (ttsReady) {
                                    tts.speak(state.currentItem?.word.orEmpty())
                                }
                            },
                            onStop = { tts.stop() },
                        )
                    }
                } else {
                    {}
                },
            )

            StudyCanvas(
                sessionStage = sessionStage,
                currentItem = currentItem,
                cardFace = cardFace,
                onTapCard = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    cardFace = cardFace.next
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            StudyActionDock(
                sessionStage = sessionStage,
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
            )
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
    cardFace: CardFace,
    onTapCard: () -> Unit,
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
                cardFace = cardFace,
                onTapCard = onTapCard,
            )
        }
    }
}

@Composable
private fun StudyCardStage(
    currentItem: StudySessionItem?,
    cardFace: CardFace,
    onTapCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        modifier = modifier.fillMaxSize(),
    ) { item ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onTapCard,
                ),
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedContent(
                targetState = cardFace,
                transitionSpec = {
                    fadeIn(tween(CARD_TRANSITION_DURATION_MS)) togetherWith fadeOut(tween(CARD_EXIT_FADE_DURATION_MS))
                },
                label = "card_face",
            ) { face ->
                when (face) {
                    CardFace.Front -> FlashcardFront(
                        word = item?.word.orEmpty(),
                        phonetic = item?.phonetic.orEmpty(),
                    )
                    CardFace.Back -> item?.let { FlashcardBack(item = it) }
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
    val isSpeaking: Boolean = audioState.isSpeaking
    val description: String = stringResource(
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
    onRevealAnswer: () -> Unit,
    onReviewAnswer: (ReviewGrade) -> Unit,
    onCreateCard: () -> Unit,
    onRetryLoad: () -> Unit,
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
        modifier = modifier.fillMaxWidth(),
    ) { stage ->
        when (stage) {
            StudyStage.Loading -> Unit

            StudyStage.Error -> {
                HButton(
                    text = stringResource(R.string.study_error_retry),
                    onClick = onRetryLoad,
                    variant = HButtonVariant.Primary,
                    full = true,
                )
            }

            StudyStage.Empty -> {
                HButton(
                    text = stringResource(R.string.study_empty_create_card_cta),
                    onClick = onCreateCard,
                    variant = HButtonVariant.Secondary,
                    full = true,
                )
            }

            StudyStage.Recall -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HButton(
                        text = stringResource(R.string.study_recall_cta_reveal),
                        onClick = onRevealAnswer,
                        variant = HButtonVariant.Primary,
                        full = true,
                    )
                    Text(
                        text = stringResource(R.string.study_recall_hint),
                        fontFamily = schibsted,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = inkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            StudyStage.Grade -> {
                AnswerButtons(onReviewAnswer = onReviewAnswer)
            }
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.study_prompt_label).uppercase(),
            fontFamily = schibsted,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.12.em,
            color = inkMuted,
        )
        Text(
            text = word,
            fontFamily = bricolage,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 52.sp,
            lineHeight = 54.sp,
            letterSpacing = (-0.02).em,
            color = ink,
        )
        if (phonetic.isNotBlank()) {
            Text(
                text = phonetic,
                fontFamily = schibsted,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = inkMuted,
            )
        }
    }
}

@Composable
private fun FlashcardBack(item: StudySessionItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = item.word,
            fontFamily = schibsted,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = inkMuted,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = item.translation,
                fontFamily = bricolage,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                lineHeight = 46.sp,
                letterSpacing = (-0.02).em,
                color = ink,
            )
        }
        if (item.example.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = highlightWordInExample(item.example, item.word),
                    fontFamily = schibsted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    color = ink,
                )
                if (item.exampleTranslation.isNotBlank()) {
                    Text(
                        text = item.exampleTranslation,
                        fontFamily = schibsted,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = inkSoft,
                    )
                }
            }
        }
        val referenceLine: String = listOf(item.partOfSpeech, item.phonetic, item.meaning)
            .filter(String::isNotBlank)
            .joinToString(" · ")
        if (referenceLine.isNotBlank()) {
            Text(
                text = referenceLine,
                fontFamily = schibsted,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = inkMuted,
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
        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GradeForgotButton(
            onClick = { onReviewAnswer(ReviewGrade.AGAIN) },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = gradeButtonMinHeight),
        )
        GradeKnewButton(
            onClick = { onReviewAnswer(ReviewGrade.GOOD) },
            onLongClick = { onReviewAnswer(ReviewGrade.EASY) },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = gradeButtonMinHeight),
        )
    }
}

@Composable
private fun GradeForgotButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape: RoundedCornerShape = MaterialTheme.helloShapes.control
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Transparent, shape)
            .border(1.dp, outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.study_grade_forgot),
            fontFamily = schibsted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
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
    val shape: RoundedCornerShape = MaterialTheme.helloShapes.control
    val haptics: HapticFeedback = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(ink, shape)
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
            fontFamily = bricolage,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = onInk,
        )
    }
}

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

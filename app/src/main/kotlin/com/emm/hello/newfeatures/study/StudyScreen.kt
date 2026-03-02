package com.emm.hello.newfeatures.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onReviewAnswer: (Flashcard?, ReviewGrade) -> Unit = { _, _ -> },
    state: StudyUiState = StudyUiState(),
) {
    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }
    val tts: TextToSpeechController = rememberTextToSpeech()
    var isSpeaking by remember { mutableStateOf(false) }

    val prevFlashCard = remember { mutableStateOf(state.currentFlashcard) }
    var cardFace by remember { mutableStateOf(CardFace.Front) }

    val progress = if (state.totalCount > 0) {
        state.reviewedCount.toFloat() / state.totalCount.toFloat()
    } else 0f

    LaunchedEffect(state.currentFlashcard?.id) { cardFace = CardFace.Front }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) setShowDialog(true)
    }

    DisposableEffect(tts) {
        tts.onDoneSpeaking = { isSpeaking = false }
        onDispose { tts.onDoneSpeaking = null }
    }

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
                                "Repaso",
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
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Salir de la sesión",
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
            // ── Barra de progreso de la sesión ───────────────────────────────
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // ── Indicador de cara ────────────────────────────────────────
                Text(
                    text = if (cardFace == CardFace.Front) "Toca para revelar" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )

                // ── Tarjeta animada ──────────────────────────────────────────
                FlippableCard(
                    cardFace = cardFace,
                    onClick = { cardFace = it.next },
                    onFinished = { prevFlashCard.value = state.currentFlashcard },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    frontContent = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp),
                            ) {
                                Text(
                                    text = state.currentFlashcard?.word.orEmpty(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                )
                                if (state.currentFlashcard?.phonetic?.isNotBlank() == true) {
                                    Text(
                                        text = state.currentFlashcard.phonetic,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    },
                    backContent = {
                        FlashcardBackContent(
                            card = prevFlashCard.value,
                            isSpeaking = isSpeaking,
                            ttsReady = tts.isReady,
                            onStop = {
                                isSpeaking = false
                                tts.stop()
                            },
                            onSpeak = {
                                if (tts.isReady) {
                                    isSpeaking = true
                                    tts.speak(state.currentFlashcard?.word.orEmpty())
                                }
                            },
                        )
                    },
                )

                // ── Botones de respuesta ─────────────────────────────────────
                AnimatedContent(
                    targetState = cardFace == CardFace.Back,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    },
                    label = "answer_buttons",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) { showButtons ->
                    if (showButtons) {
                        AnswerButtons { grade -> onReviewAnswer(state.currentFlashcard, grade) }
                    } else {
                        Spacer(Modifier.height(104.dp))
                    }
                }
            }
        }
    }

    if (showDialog) {
        HAlertDialog(
            title = "¡Sesión completada! 🎉",
            description = "Has repasado ${state.totalCount} tarjetas. ¡Sigue así!",
            icon = Icons.Outlined.Check,
            confirmText = "Volver",
            cancelText = null,
            onConfirm = { setShowDialog(false); onNavigateBack() },
            onDismiss = { setShowDialog(false); onNavigateBack() },
        )
    }
}

// ── Contenido del reverso de la tarjeta ──────────────────────────────────────

@Composable
private fun FlashcardBackContent(
    card: Flashcard?,
    isSpeaking: Boolean,
    ttsReady: Boolean,
    onStop: () -> Unit = {},
    onSpeak: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = card?.translation.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = card?.meaning.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        IconButton(
            onClick = { if (isSpeaking) onStop() else onSpeak() },
            enabled = ttsReady,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isSpeaking) "Detener" else "Pronunciar",
                tint = if (isSpeaking) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Botones de calificación ───────────────────────────────────────────────────

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
                text = "Otra vez",
                onClick = { onReviewAnswer(ReviewGrade.AGAIN) },
                variant = ButtonVariant.Destructive,
                modifier = Modifier.weight(1f),
            )
            HButton(
                text = "Difícil",
                onClick = { onReviewAnswer(ReviewGrade.HARD) },
                variant = ButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HButton(
                text = "Bien",
                onClick = { onReviewAnswer(ReviewGrade.GOOD) },
                variant = ButtonVariant.Default,
                modifier = Modifier.weight(1f),
            )
            HButton(
                text = "Fácil",
                onClick = { onReviewAnswer(ReviewGrade.EASY) },
                variant = ButtonVariant.Outline,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── TTS helper ────────────────────────────────────────────────────────────────

@Composable
private fun rememberTextToSpeech(): TextToSpeechController {
    val context = LocalContext.current
    val controller = remember { TextToSpeechController(context) }
    DisposableEffect(Unit) {
        controller.init()
        onDispose { controller.shutdown() }
    }
    return controller
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun StudyScreenPreview() {
    HelloTheme {
        StudyScreen(
            state = StudyUiState(
                currentFlashcard = Flashcard(
                    id = "1",
                    word = "Serendipity",
                    meaning = "The occurrence of events by chance in a happy way",
                    translation = "Casualidad afortunada",
                    examples = listOf(),
                    phonetic = "/ˌserənˈdɪpɪti/",
                    review = FlashcardReview.Empty,
                ),
                reviewedCount = 3,
                totalCount = 10,
            ),
        )
    }
}
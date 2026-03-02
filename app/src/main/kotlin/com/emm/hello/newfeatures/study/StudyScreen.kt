package com.emm.hello.newfeatures.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
    val isSpeaking: MutableState<Boolean> = remember { mutableStateOf(false) }

    val prevFlashCard = remember { mutableStateOf(state.currentFlashcard) }
    var cardFace by remember { mutableStateOf(CardFace.Front) }

    LaunchedEffect(state.currentFlashcard?.id) { cardFace = CardFace.Front }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) setShowDialog(true)
    }

    DisposableEffect(tts) {
        tts.onDoneSpeaking = { isSpeaking.value = false }
        onDispose { tts.onDoneSpeaking = null }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Repaso")
                        HBadge(
                            label = "${state.reviewedCount}/${state.totalCount}",
                            variant = BadgeVariant.Secondary,
                        )
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            FlippableCard(
                cardFace = cardFace,
                onClick = { cardFace = it.next },
                onFinished = { prevFlashCard.value = state.currentFlashcard },
                modifier = Modifier
                    .weight(1f)
                    .size(300.dp),
                frontContent = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.currentFlashcard?.word.orEmpty(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                },
                backContent = {
                    FlashcardContent(
                        card = prevFlashCard.value,
                        isSpeaking = isSpeaking.value,
                        enabled = tts.isReady,
                        onStop = { isSpeaking.value = false; tts.stop() },
                        onSpeak = {
                            if (tts.isReady) {
                                isSpeaking.value = true
                                tts.speak(state.currentFlashcard?.word.orEmpty())
                            }
                        },
                    )
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f),
                contentAlignment = Alignment.Center,
            ) {
                if (cardFace == CardFace.Back) {
                    AnswerButtons { reviewGrade -> onReviewAnswer(state.currentFlashcard, reviewGrade) }
                }
            }
        }
    }

    if (showDialog) {
        HAlertDialog(
            title = "Sesión de repaso completada",
            description = "¡Bien hecho! Has repasado todas las tarjetas de esta sesión.",
            icon = Icons.Outlined.Check,
            confirmText = "Volver",
            cancelText = null,
            onConfirm = { setShowDialog(false); onNavigateBack() },
            onDismiss = { setShowDialog(false); onNavigateBack() },
        )
    }
}

@Composable
private fun FlashcardContent(
    card: Flashcard?,
    isSpeaking: Boolean,
    enabled: Boolean,
    onStop: () -> Unit = {},
    onSpeak: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = card?.translation.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Text(
            text = card?.meaning.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        IconButton(
            onClick = { if (isSpeaking) onStop() else onSpeak() },
            enabled = enabled,
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

@Composable
fun rememberTextToSpeech(): TextToSpeechController {
    val context = LocalContext.current
    val controller = remember { TextToSpeechController(context) }
    DisposableEffect(Unit) {
        controller.init()
        onDispose { controller.shutdown() }
    }
    return controller
}

@Composable
fun AnswerButtons(
    modifier: Modifier = Modifier,
    onReviewAnswer: (ReviewGrade) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HButton(
                text = "Again",
                onClick = { onReviewAnswer(ReviewGrade.AGAIN) },
                variant = ButtonVariant.Destructive,
                modifier = Modifier.weight(1f),
            )
            HButton(
                text = "Hard",
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
                text = "Good",
                onClick = { onReviewAnswer(ReviewGrade.GOOD) },
                variant = ButtonVariant.Default,
                modifier = Modifier.weight(1f),
            )
            HButton(
                text = "Easy",
                onClick = { onReviewAnswer(ReviewGrade.EASY) },
                variant = ButtonVariant.Outline,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@PreviewLightDark
@Composable
fun StudyScreenPreviewLight() {
    HelloTheme(darkTheme = false) {
        StudyScreen(
            state = StudyUiState(
                currentFlashcard = Flashcard(
                    id = "Hello",
                    word = "Serendipity",
                    meaning = "The occurrence of events by chance in a happy way",
                    translation = "Casualidad afortunada",
                    examples = listOf(),
                    phonetic = "/ˌserənˈdɪpɪti/",
                    review = FlashcardReview.Empty,
                ),
                reviewedCount = 3,
                totalCount = 10,
            )
        )
    }
}
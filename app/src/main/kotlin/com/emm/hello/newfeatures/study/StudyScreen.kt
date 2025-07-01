package com.emm.hello.newfeatures.study

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.hello.core.theme.HelloTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onReviewAnswer: (Flashcard?, ReviewGrade) -> Unit = { _, _ -> },
    state: StudyUiState = StudyUiState(),
) {

    var isFlipped by remember { mutableStateOf(false) }
    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }

    LaunchedEffect(state.currentFlashcard?.id) {
        isFlipped = false
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            setShowDialog(true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Sesión de Repaso") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Salir de la sesión")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Flashcard(
                card = state.currentFlashcard,
                isFlipped = isFlipped,
                onFlip = { isFlipped = !isFlipped }
            )

            if (isFlipped) {
                Spacer(Modifier.height(24.dp))
                AnswerButtons { reviewGrade ->
                    onReviewAnswer(state.currentFlashcard, reviewGrade)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { setShowDialog(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        setShowDialog(false)
                        onNavigateBack()
                    }
                ) {
                    Text(text = "Aceptar", color = MaterialTheme.colorScheme.primary)
                }
            },
            title = { Text(text = "Sesión de repaso") },
            text = { Text(text = "Has terminado la sesión de repaso") },
            icon = { Icon(Icons.Outlined.Check, contentDescription = "Example Icon") },
        )
    }
}

@Composable
fun Flashcard(
    card: Flashcard?,
    isFlipped: Boolean,
    onFlip: () -> Unit
) {
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "card_flip_animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 12 * density
            }
            .clickable { onFlip() },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (rotationY <= 90f) {
                // Frente
                Text(
                    text = card?.word.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // Reverso
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .graphicsLayer { scaleX = -1f }, // Corrige el efecto espejo
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = card?.translation.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Pronunciación")
                        }
                        TextButton(onClick = { }) {
                            Text("📝 Añadir nota")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnswerButtons(onReviewAnswer: (ReviewGrade) -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Button(
            onClick = { onReviewAnswer(ReviewGrade.AGAIN) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            content = { Text("Again") }
        )
        Button(
            onClick = { onReviewAnswer(ReviewGrade.HARD) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
            content = { Text("Hard") }
        )
        Button(
            onClick = { onReviewAnswer(ReviewGrade.GOOD) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
            content = { Text("Good") }
        )
        Button(
            onClick = { onReviewAnswer(ReviewGrade.EASY) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF689F38)),
            content = { Text("Easy") }
        )
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
                    word = "Hola",
                    meaning = "nec",
                    translation = "partiendo",
                    examples = listOf(),
                    phonetic = "(831) 768-0261",
                    review = FlashcardReview.Empty
                )
            )
        )
    }
}
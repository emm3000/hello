package com.emm.hello.newfeatures

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

// Data class para la tarjeta de estudio
data class StudyCard(val question: String, val answer: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(modifier: Modifier = Modifier, onExit: () -> Unit = {}) {
    var isFlipped by remember { mutableStateOf(false) }
    val card = StudyCard(
        question = "What is Jetpack Compose?",
        answer = "A modern toolkit for building native Android UI. It simplifies and accelerates UI development on Android."
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Sesión de Repaso") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Salir de la sesión")
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
                card = card,
                isFlipped = isFlipped,
                onFlip = { isFlipped = !isFlipped }
            )

            if (isFlipped) {
                Spacer(Modifier.height(24.dp))
                AnswerButtons()
            }
        }
    }
}

@Composable
fun Flashcard(
    card: StudyCard,
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
                    text = card.question,
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
                        text = card.answer,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { /* TODO: Pronunciar texto */ }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Pronunciación")
                        }
                        TextButton(onClick = { /* TODO: Añadir nota */ }) {
                            Text("📝 Añadir nota")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnswerButtons() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Button(onClick = { /* TODO */ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))) { Text("Again") }
        Button(onClick = { /* TODO */ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))) { Text("Hard") }
        Button(onClick = { /* TODO */ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))) { Text("Good") }
        Button(onClick = { /* TODO */ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF689F38))) { Text("Easy") }
    }
}

@Preview(showBackground = true, name = "Study Screen Light")
@Composable
fun StudyScreenPreviewLight() {
    HelloTheme(darkTheme = false) {
        StudyScreen()
    }
}

@Preview(showBackground = true, name = "Study Screen Dark")
@Composable
fun StudyScreenPreviewDark() {
    HelloTheme(darkTheme = true) {
        StudyScreen()
    }
}
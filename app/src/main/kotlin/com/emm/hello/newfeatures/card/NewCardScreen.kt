package com.emm.hello.newfeatures.card

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.SwitchLeft
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.TypeView
import com.emm.domain.flashcard.difficult
import com.emm.domain.flashcard.staticCategories
import com.emm.hello.core.audio.rememberSpeechToTextManager
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.CardVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HCard
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HSelect
import com.emm.hello.core.ui.HSeparator
import java.time.LocalDateTime
import java.util.Locale

@Composable
fun NewCardScreen(
    modifier: Modifier = Modifier,
    state: NewCardUiState = NewCardUiState(),
    onAction: (NewCardAction) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val sttManager = rememberSpeechToTextManager { voiceText ->
        onAction(NewCardAction.WordChanged(voiceText))
    }
    val isListening by sttManager.isListening.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) sttManager.startListening(Locale.US)
        }
    )

    val toggleVoiceInput = {
        if (isListening) {
            sttManager.stopListening()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                sttManager.startListening(Locale.US)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val showBottomSheet = remember { mutableStateOf(false) }
    val isGenerateEnabled = remember(state.isLoading, state.deckSelected, state.word, state.typeView) {
        val hasWord = state.word.isNotBlank()
        val hasDeck = state.deckSelected != null
        val notLoading = !state.isLoading
        when (state.typeView) {
            TypeView.WordOrPhase -> notLoading && hasDeck && hasWord
            TypeView.WithCategories -> notLoading && hasDeck
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Nueva tarjeta",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = when (state.typeView) {
                                TypeView.WordOrPhase -> "Palabra o frase"
                                TypeView.WithCategories -> "Por categoría"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onAction(NewCardAction.TypeViewSelected(state.typeView.other)) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwitchLeft,
                            contentDescription = "Cambiar vista",
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // -- Input Section --------------------------------------------------------
            item {
                SectionCard(title = "Entrada") {
                    when (state.typeView) {
                        TypeView.WordOrPhase -> {
                            HInput(
                                value = state.word,
                                onValueChange = { onAction(NewCardAction.WordChanged(it)) },
                                enabled = !state.isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                label = "Palabra o frase en inglés",
                                placeholder = if (isListening) "Escuchando…" else "Ej: Hello, Good morning…",
                                trailingIcon = {
                                    VoiceInputButton(
                                        isListening = isListening,
                                        onClick = toggleVoiceInput
                                    )
                                }
                            )
                        }
                        TypeView.WithCategories -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                JustClickableInput(
                                    value = state.category.name,
                                    label = "Categoría",
                                    onClick = { showBottomSheet.value = true },
                                )
                                HSelect(
                                    modifier = Modifier.fillMaxWidth(),
                                    label = "Dificultad",
                                    items = difficult,
                                    itemSelected = state.difficulty,
                                    onItemSelected = { onAction(NewCardAction.DifficultySelected(it)) },
                                )
                            }
                        }
                    }
                }
            }

            // -- Destination Section --------------------------------------------------
            item {
                SectionCard(title = "Destino") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HSelect(
                            items = state.decks,
                            itemSelected = state.deckSelected,
                            enabled = !state.isLoading,
                            onItemSelected = { onAction(NewCardAction.DeckSelected(it)) },
                            label = "Mazo",
                            placeholder = "Seleccionar mazo…",
                            itemLabel = { it.name },
                        )

                        AnimatedVisibility(
                            visible = state.decks.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            LabeledCheckbox(
                                label = "Marcar como mazo por defecto",
                                checked = state.isCheck,
                                isEnabled = state.deckSelected != null,
                                onCheckedChange = { onAction(NewCardAction.CheckChanged(it)) },
                            )
                        }
                    }
                }
            }

            // -- Generate Button ------------------------------------------------------
            item {
                HButton(
                    text = "Generar",
                    onClick = { onAction(NewCardAction.GenerateClicked) },
                    enabled = isGenerateEnabled,
                    isLoading = state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                )
            }

            // -- Error Display --------------------------------------------------------
            if (state.error != null) {
                item {
                    HAlert(
                        title = "Error al generar",
                        description = state.error,
                        variant = AlertVariant.Destructive,
                    )
                }
            }

            // -- Result Preview -------------------------------------------------------
            if (state.result != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Resultado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        CardPreview(state.result)
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    BottomSheetDialogForPickCategory(
        onDismissRequest = { showBottomSheet.value = it },
        showBottomSheet = showBottomSheet.value,
        accounts = staticCategories,
        onAction = { onAction(NewCardAction.CategorySelected(it)) },
    )
}

// -- Voice Components ---------------------------------------------------------

@Composable
private fun VoiceInputButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier.scale(scale)
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
            contentDescription = "Entrada de voz",
            tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

// -- Internal Composables -----------------------------------------------------

@Composable
private fun LabeledCheckbox(
    label: String,
    checked: Boolean,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange(!checked) }, enabled = isEnabled)
            .padding(8.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CardPreview(flashcard: Flashcard) {
    HCard(variant = CardVariant.Outlined) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = flashcard.word,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = flashcard.phonetic,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HSeparator()

            InfoRow(label = "Traducción", value = flashcard.translation)
            InfoRow(label = "Significado", value = flashcard.meaning)

            if (flashcard.examples.isNotEmpty()) {
                HSeparator()
                Text(
                    text = "Ejemplos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    flashcard.examples.forEachIndexed { index, example ->
                        key(example.exampleId) {
                            ExampleItem(index = index + 1, example = example)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        content()
        HSeparator()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ExampleItem(index: Int, example: Example) {
    var showTranslation by remember(example.exampleId) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$index.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = example.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }

        AnimatedVisibility(visible = showTranslation) {
            Text(
                text = example.translation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp),
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            HButton(
                text = if (showTranslation) "Ocultar" else "Ver traducción",
                onClick = { showTranslation = !showTranslation },
                variant = ButtonVariant.Ghost,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NewCardScreenPreview() {
    HelloTheme {
        NewCardScreen(
            state = NewCardUiState(
                word = "Serendipity",
                decks = listOf(
                    Deck(
                        id = "1",
                        name = "Vocabulario Inglés",
                        description = "",
                        createdAt = LocalDateTime.now(),
                        cards = listOf(),
                        cardsCount = 24,
                    )
                ),
                result = Flashcard(
                    id = "1",
                    word = "Serendipity",
                    meaning = "The occurrence of events by chance in a happy way",
                    translation = "Casualidad afortunada",
                    examples = listOf(
                        Example(
                            "ex1",
                            "Finding that book was pure serendipity",
                            "Encontrar ese libro fue pura casualidad afortunada",
                            ""
                        ),
                        Example("ex2", "She called it serendipity", "Ella lo llamó serendipia", ""),
                    ),
                    phonetic = "/ˌserənˈdɪpɪti/",
                    review = FlashcardReview.Empty,
                )
            )
        )
    }
}

package com.emm.hello.newfeatures.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwitchLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.TypeView
import com.emm.domain.flashcard.difficult
import com.emm.domain.flashcard.staticCategories
import com.emm.hello.core.theme.HelloTheme
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCardScreen(
    modifier: Modifier = Modifier,
    state: NewCardUiState = NewCardUiState(),
    onAction: (NewCardAction) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {

    val showBottomSheet = remember { mutableStateOf(false) }
    val isGenerateEnabled by remember(state.isLoading, state.deckSelected, state.word, state.typeView) {
        derivedStateOf {
            (!state.isLoading && state.deckSelected != null && state.word.isNotBlank()) ||
                (state.typeView == TypeView.WithCategories && !state.isLoading && state.deckSelected != null)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Crear nueva tarjeta", 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = when(state.typeView) {
                                TypeView.WordOrPhase -> "Modo: Palabra o frase"
                                TypeView.WithCategories -> "Modo: Por categoría"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        onClick = {
                            onAction(NewCardAction.OnTypeViewSelected(state.typeView.other))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwitchLeft,
                            contentDescription = "Cambiar vista"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 100.dp
            ),
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            item { 
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -20 })
                ) {
                    SectionCard(title = "📝 Entrada") {

                        when(state.typeView) {
                            TypeView.WordOrPhase -> {
                                OutlinedTextField(
                                    value = state.word,
                                    onValueChange = { onAction(NewCardAction.OnWordChanged(it)) },
                                    enabled = !state.isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Palabra o frase en inglés") },
                                    placeholder = { Text("Ej: Hello, Good morning...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            TypeView.WithCategories -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    JustClickableInput(
                                        value = state.category.name,
                                        label = "Categoría",
                                        onClick = { showBottomSheet.value = true }
                                    )
                                    GemaDropdown(
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = true,
                                        textLabel = "Dificultad",
                                        items = difficult,
                                        itemSelected = state.difficulty,
                                        onItemSelected = {
                                            onAction(NewCardAction.OnDifficultySelected(it))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { 
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 100)) + 
                           slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(300, delayMillis = 100))
                ) {
                    SectionCard(title = "🎯 Destino") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DeckSelector(
                                decks = state.decks,
                                selected = state.deckSelected,
                                enabled = state.isLoading.not(),
                                onSelected = { onAction(NewCardAction.OnDeckSelected(it)) }
                            )
                            
                            AnimatedVisibility(
                                visible = state.decks.isNotEmpty(),
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                LabeledCheckbox(
                                    label = "Marcar como deck por defecto",
                                    checked = state.isCheck,
                                    isEnabled = state.deckSelected != null,
                                    onCheckedChange = {
                                        onAction(NewCardAction.OnCheckChanged(it))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item { 
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 200)) + 
                           slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(300, delayMillis = 200))
                ) {
                    Button(
                        onClick = {
                            onAction(NewCardAction.OnGenerateClicked)
                        },
                        enabled = isGenerateEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        if (state.isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, 
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp), 
                                    color = LocalContentColor.current, 
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    "Generando con IA…",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🤖")
                                Text(
                                    "Generar con IA",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }

            if (state.result != null) {
                item { 
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(400)) + 
                               slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(400))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "✨ Resultado",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            CardPreview(state.result)
                        }
                    }
                }
            }

            if (state.error != null) {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "⚠️",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = state.error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

        }
    }

    BottomSheetDialogForPickCategory(
        onDismissRequest = { showBottomSheet.value = it },
        showBottomSheet = showBottomSheet.value,
        accounts = staticCategories,
        onAction = { onAction(NewCardAction.OnCategorySelected(it)) }
    )
}

@Composable
fun LabeledCheckbox(
    label: String,
    checked: Boolean,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = { onCheckedChange(!checked) },
                enabled = isEnabled
            )
            .padding(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}

@Composable
fun CardPreview(flashcard: Flashcard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = flashcard.word,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = flashcard.phonetic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            InfoRow(label = "Traducción", value = flashcard.translation)
            InfoRow(label = "Significado", value = flashcard.meaning)

            if (flashcard.examples.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                Text(
                    text = "📚 Ejemplos de uso",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ExampleItem(index: Int, example: Example) {
    var showTranslation by remember(example.exampleId) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = example.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(
                visible = showTranslation,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Text(
                    text = example.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showTranslation = !showTranslation },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (showTranslation) "Ocultar" else "Ver traducción",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardPreviewPreview() {
    HelloTheme {
        val sampleFlashcard = Flashcard(
            id = "1",
            word = "Hello",
            meaning = "A greeting or salutation.",
            translation = "Hola",
            examples = listOf(
                Example("ex1", "Hello, how are you?", "¿Hola como estas?", ""),
                Example("ex2", "She said hello to him.", "Ella le dijo hola.", "")
            ),
            phonetic = "/həˈloʊ/",
            review = FlashcardReview.Empty,
        )
        CardPreview(flashcard = sampleFlashcard)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckSelector(
    decks: List<Deck>,
    selected: Deck?,
    enabled: Boolean,
    onSelected: (Deck) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("📂 Mazo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            decks.forEach { deck ->
                DropdownMenuItem(
                    text = { Text(deck.name) },
                    onClick = {
                        onSelected(deck)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NewCardScreenPreview() {
    HelloTheme {
        NewCardScreen(
            state = NewCardUiState(
                word = "word",
                decks = listOf(
                    Deck(
                        id = "doctus",
                        name = "Joshua Maxwell",
                        description = "idque",
                        createdAt = LocalDateTime.now(),
                        cards = listOf(),
                        cardsCount = 4080
                    )
                ),
                result = Flashcard(
                    id = "dictumst",
                    word = "utroque",
                    meaning = "porro",
                    translation = "est",
                    examples = listOf(
                        Example(
                            exampleId = "facilisis",
                            text = "adversarium",
                            translation = "montes",
                            type = "petentium"
                        ),
                        Example(
                            exampleId = "facilisis1",
                            text = "adversarium",
                            translation = "montes",
                            type = "petentium"
                        ),
                        Example(
                            exampleId = "facilisis2",
                            text = "adversarium",
                            translation = "montes",
                            type = "petentium"
                        )
                    ),
                    phonetic = "(608) 847-7529",
                    review = FlashcardReview(
                        flashcardId = "pri",
                        lastReviewedAt = 1816,
                        nextReviewAt = 3409,
                        easeFactor = 2.3,
                        interval = 8173,
                        repetitions = 7428,
                        lapses = 9002
                    )
                )
            )
        )
    }
}
package com.emm.hello.newfeatures.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwitchLeft
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Crear Nueva Tarjeta") },
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
                            contentDescription = "Change Ui"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            when(state.typeView) {
                TypeView.WordOrPhase -> item {
                    OutlinedTextField(
                        value = state.word,
                        onValueChange = { onAction(NewCardAction.OnWordChanged(it)) },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Palabra o frase en inglés") },
                        singleLine = true
                    )
                }
                TypeView.WithCategories -> item {
                    JustClickableInput(
                        value = state.category.name,
                        label = "Category",
                        onClick = { showBottomSheet.value = true }
                    )
                    GemaDropdown(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = true,
                        textLabel = "Difficult",
                        items = difficult,
                        itemSelected = state.difficulty,
                        onItemSelected = {
                            onAction(NewCardAction.OnDifficultySelected(it))
                        }
                    )
                }
            }

            item {
                Column {
                    DeckSelector(
                        decks = state.decks,
                        selected = state.deckSelected,
                        enabled = state.isLoading.not(),
                        onSelected = { onAction(NewCardAction.OnDeckSelected(it)) }
                    )
                    if (state.decks.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        LabeledCheckbox(
                            label = "Marcar deck por defecto",
                            checked = state.isCheck,
                            isEnabled = state.deckSelected != null,
                            onCheckedChange = {
                                onAction(NewCardAction.OnCheckChanged(it))
                            }
                        )
                    }
                }

            }

            item {
                Button(
                    onClick = {
                        onAction(NewCardAction.OnGenerateClicked)
                    },
                    enabled = !state.isLoading && state.deckSelected != null && state.word.isNotBlank() || (state.typeView == TypeView.WithCategories && !state.isLoading),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = LocalContentColor.current)
                    } else {
                        Text("🤖 Generar con IA")
                    }
                }
            }

            if (state.result != null) {
                item { CardPreview(state.result) }
            }

            if (state.error != null) {
                item {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error
                    )
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
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = flashcard.word,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = flashcard.phonetic,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Traducción: ${flashcard.translation}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Significado: ${flashcard.meaning}",
                style = MaterialTheme.typography.bodyLarge
            )

            if (flashcard.examples.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ejemplos:",
                    style = MaterialTheme.typography.titleMedium
                )
                flashcard.examples.forEachIndexed { index, example ->
                    key(example.exampleId) {
                        ExampleItem(index = index + 1, example = example)
                        if (index < flashcard.examples.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExampleItem(index: Int, example: Example) {
    var showTranslation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$index. ${example.text}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (showTranslation) {
            Text(
                text = example.translation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showTranslation = !showTranslation }) {
                Text(if (showTranslation) "Ocultar traducción" else "Ver traducción")
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
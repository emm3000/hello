package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.emm.hello.core.theme.HelloTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCardScreen(
    modifier: Modifier = Modifier,
    state: NewCardUiState = NewCardUiState(),
    onAction: (NewCardAction) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Crear Nueva Tarjeta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.word,
                    onValueChange = { onAction(NewCardAction.OnWordChanged(it)) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Palabra o frase en inglés") },
                    singleLine = true
                )
            }

            item {
                DeckSelector(
                    decks = state.decks,
                    selected = state.deckSelected,
                    enabled = state.isLoading.not(),
                    onSelected = { onAction(NewCardAction.OnDeckSelected(it)) }
                )
            }

            item {
                Button(
                    onClick = {
                        onAction(NewCardAction.OnGenerateClicked)
                    },
                    enabled = !state.isLoading && state.deckSelected != null && state.word.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
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
            phonetic = "/həˈloʊ/"
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

@Preview(showBackground = true)
@Composable
fun NewCardScreenPreview() {
    HelloTheme {
        NewCardScreen()
    }
}
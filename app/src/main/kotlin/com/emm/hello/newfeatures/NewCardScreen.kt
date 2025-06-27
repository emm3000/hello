package com.emm.hello.newfeatures

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

// Data class para los detalles de la tarjeta
data class CardData(
    val word: String = "",
    val meaning: String = "",
    val translation: String = "",
    val example: String = "",
    val phonetic: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCardScreen(modifier: Modifier = Modifier, onNavigateBack: () -> Unit = {}) {
    var userInput by remember { mutableStateOf("") }
    var cardData by remember { mutableStateOf(CardData()) }
    var isGenerating by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    val decks = listOf("Inglés B2", "Phrasal Verbs", "Ciencia")
    var selectedDeck by remember { mutableStateOf(decks.first()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Crear Nueva Tarjeta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
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
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Palabra o frase en inglés") },
                    singleLine = true
                )
            }

            item {
                Button(onClick = { 
                    isGenerating = true 
                    showPreview = true
                    // TODO: Simular llamada a IA y actualizar cardData
                }) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("🤖 Generar con IA")
                    }
                }
            }

            if (showPreview) {
                item { CardPreview(cardData = cardData, onDataChange = { cardData = it }) }
            }
            
            item { DeckSelector(decks = decks, selected = selectedDeck, onSelected = { selectedDeck = it }) }

            item {
                OutlinedTextField(
                    value = "",
                    onValueChange = { /* TODO */ },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("🏷 Etiquetas (ej: gramática, verbo)") }
                )
            }

            item {
                Button(
                    onClick = { /* TODO: Guardar tarjeta */ },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Text("💾 Guardar Tarjeta")
                }
            }
        }
    }
}

@Composable
fun CardPreview(cardData: CardData, onDataChange: (CardData) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = cardData.word, onValueChange = { onDataChange(cardData.copy(word = it)) }, label = { Text("Word") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = cardData.meaning, onValueChange = { onDataChange(cardData.copy(meaning = it)) }, label = { Text("Meaning") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = cardData.translation, onValueChange = { onDataChange(cardData.copy(translation = it)) }, label = { Text("Translation") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = cardData.example, onValueChange = { onDataChange(cardData.copy(example = it)) }, label = { Text("Example") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = cardData.phonetic, onValueChange = { onDataChange(cardData.copy(phonetic = it)) }, label = { Text("Phonetic") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckSelector(decks: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {}, 
            readOnly = true,
            label = { Text("📂 Mazo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            decks.forEach { deck ->
                DropdownMenuItem(
                    text = { Text(deck) },
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
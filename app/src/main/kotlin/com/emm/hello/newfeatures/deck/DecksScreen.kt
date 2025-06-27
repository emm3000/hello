package com.emm.hello.newfeatures.deck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

// Data class para representar un mazo
data class DeckInfo(val name: String, val cardCount: Int, val completion: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(modifier: Modifier = Modifier, onDeckClick: (DeckInfo) -> Unit = {}) {
    val decks = listOf(
        DeckInfo("Vocabulario de Inglés", 100, 0.15f),
        DeckInfo("Conceptos de Kotlin", 80, 0.62f),
        DeckInfo("Historia del Arte", 150, 0.50f),
        DeckInfo("Phrasal Verbs", 50, 0.90f)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("📚 Mis Mazos") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Abrir pantalla de nuevo mazo */ }) {
                Icon(Icons.Default.Add, contentDescription = "Crear nuevo mazo")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(decks) { deck ->
                DeckListItem(deck = deck, onClick = { onDeckClick(deck) })
            }
        }
    }
}

@Composable
fun DeckListItem(deck: DeckInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(deck.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${deck.cardCount} tarjetas", style = MaterialTheme.typography.bodyMedium)
                Text("${(deck.completion * 100).toInt()}% completado", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = deck.completion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small)
            )
        }
    }
}

@Preview(showBackground = true, name = "Decks Screen Light")
@Composable
fun DecksScreenPreviewLight() {
    HelloTheme(darkTheme = false) {
        DecksScreen()
    }
}

@Preview(showBackground = true, name = "Decks Screen Dark")
@Composable
fun DecksScreenPreviewDark() {
    HelloTheme(darkTheme = true) {
        DecksScreen()
    }
}
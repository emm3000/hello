package com.emm.hello.newfeatures

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

// Data class para los detalles de una tarjeta en el mazo
data class CardDetail(val id: String, val word: String, val example: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    modifier: Modifier = Modifier, 
    deckName: String = "Vocabulario de Inglés",
    deckProgress: Float = 0.15f,
    onNavigateBack: () -> Unit = {}
) {
    val cards = listOf(
        CardDetail("1", "Perseverance", "Her perseverance was rewarded when she finally achieved her goal."),
        CardDetail("2", "Ephemeral", "The beauty of the cherry blossoms is ephemeral."),
        CardDetail("3", "Ubiquitous", "Smartphones have become ubiquitous in modern society.")
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(deckName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DeckDetailHeader(progress = deckProgress)
            }
            
            items(cards) { card ->
                DeckCardItem(card = card)
            }
        }
    }
}

@Composable
fun DeckDetailHeader(progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { /* TODO: Empezar repaso del mazo */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Empezar repaso de este mazo")
        }
        Spacer(Modifier.height(16.dp))
        Text("Tarjetas en este mazo", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun DeckCardItem(card: CardDetail) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(card.word, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(card.example, style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                IconButton(onClick = { /* TODO: Editar */ }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { /* TODO: Borrar */ }) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar")
                }
                IconButton(onClick = { /* TODO: Info */ }) {
                    Icon(Icons.Default.Info, contentDescription = "Información")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeckDetailScreenPreview() {
    HelloTheme {
        DeckDetailScreen()
    }
}
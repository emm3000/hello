package com.emm.hello.newfeatures.deck

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.hello.core.theme.HelloTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    modifier: Modifier = Modifier,
    deckName: String = "Vocabulario de Inglés",
    cards: List<Flashcard> = emptyList(),
    deckProgress: Float = 0.15f,
    onNavigateBack: () -> Unit = {},
    onReview: () -> Unit = {},
    onCardClick: (String) -> Unit = {}
) {

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(deckName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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
                DeckDetailHeader(progress = deckProgress) {
                    onReview()
                }
            }

            items(cards, key = Flashcard::id) { card ->
                DeckCardItem(card = card) {
                    onCardClick(card.id)
                }
            }
        }
    }
}

@Composable
fun DeckDetailHeader(progress: Float, onReview: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
//        LinearProgressIndicator(
//            progress = { progress },
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(8.dp)
//                .clip(MaterialTheme.shapes.small)
//        )
//        Spacer(Modifier.height(16.dp))
        Button(onClick = onReview, modifier = Modifier.fillMaxWidth()) {
            Text("Empezar repaso de este mazo")
        }
        Spacer(Modifier.height(16.dp))
        Text("Tarjetas en este mazo", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun DeckCardItem(card: Flashcard, onCardClick: (String) -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onCardClick(card.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(card.word, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(card.examples.firstOrNull()?.text.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeckDetailScreenPreview() {
    HelloTheme {
        DeckDetailScreen(
            cards = listOf(
                Flashcard(
                    id = "urna",
                    word = "suscipiantur",
                    meaning = "wisi",
                    translation = "consectetur",
                    examples = listOf(
                        Example(
                            exampleId = "reprimique",
                            text = "impetus",
                            translation = "appareat",
                            type = "ridens"
                        )
                    ),
                    phonetic = "(603) 760-5336",
                    review = FlashcardReview.Empty
                )
            )
        )
    }
}
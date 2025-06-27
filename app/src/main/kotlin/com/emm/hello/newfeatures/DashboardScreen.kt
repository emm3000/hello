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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

data class Deck(val name: String, val completed: Int, val total: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    newCard: () -> Unit = {},
    onDeckDetail: () -> Unit = {},
    onStartReview: () -> Unit = {},
) {
    val decks = listOf(
        Deck("Vocabulario de Inglés", 15, 100),
        Deck("Conceptos de Kotlin", 50, 80),
        Deck("Historia del Arte", 75, 150)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Hola, Edgardo 👋") },
                actions = {
                    IconButton(onClick = { /* TODO: Navegar a configuración */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { newCard() }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva tarjeta")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ReviewCard(cardsToReview = 42) {
                    onStartReview()
                }
            }

            item {
                DecksSection(decks = decks) {
                    onDeckDetail()
                }
            }

            item {
                QuoteOfTheDayCard()
            }
        }
    }
}

@Composable
fun ReviewCard(cardsToReview: Int, onStartReview: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🔔 Tienes $cardsToReview tarjetas por repasar hoy",
                style = MaterialTheme.typography.titleMedium
            )
            Button(onClick = onStartReview) {
                Text("Empezar repaso")
            }
        }
    }
}

@Composable
fun DecksSection(decks: List<Deck>, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("📚 Mis Mazos", style = MaterialTheme.typography.titleLarge)
        decks.forEach { deck ->
            DeckItem(deck = deck) {
                onClick()
            }
        }
    }
}

@Composable
fun DeckItem(deck: Deck, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(deck.name, style = MaterialTheme.typography.bodyLarge)
            Text("${deck.completed}/${deck.total}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun QuoteOfTheDayCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🧠 Frase del día jeje jaja", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "\"El aprendizaje es un tesoro que seguirá a su dueño a todas partes.\" - Proverbio chino",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true, name = "Dashboard Light")
@Composable
fun DashboardScreenPreviewLight() {
    HelloTheme(darkTheme = false) {
        DashboardScreen()
    }
}

@Preview(showBackground = true, name = "Dashboard Dark")
@Composable
fun DashboardScreenPreviewDark() {
    HelloTheme(darkTheme = true) {
        DashboardScreen()
    }
}
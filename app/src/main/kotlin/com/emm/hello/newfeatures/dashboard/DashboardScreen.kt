package com.emm.hello.newfeatures.dashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.domain.deck.Deck
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.DashboardSkeleton
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HSeparator
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    state: DashboardUiState = DashboardUiState(),
    newCard: () -> Unit = {},
    onDeckDetail: (String) -> Unit = {},
    onCreateDeck: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mazos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = newCard,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva tarjeta")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                DecksSection(onCreateDeck = onCreateDeck, decksCount = state.decks.size)
            }

            if (state.isLoading) {
                item { DashboardSkeleton(count = 4) }
            } else if (state.decks.isEmpty()) {
                item { EmptyDecks(onCreateDeck) }
            } else {
                itemsIndexed(state.decks, key = { _, deck -> deck.id }) { _, deck ->
                    DeckItem(deck = deck, onDeckClick = onDeckDetail)
                    HSeparator()
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun DecksSection(
    onCreateDeck: () -> Unit = {},
    decksCount: Int = 0,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Mazos",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (decksCount > 0) {
                HBadge(label = "$decksCount", variant = BadgeVariant.Secondary)
            }
        }
        HButton(
            text = "Nuevo mazo",
            onClick = onCreateDeck,
            variant = ButtonVariant.Ghost,
        )
    }
}

@Composable
fun DeckItem(deck: Deck, onDeckClick: (String) -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                deck.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            Text(
                text = "${deck.cardsCount} tarjetas",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDeckClick(deck.id) }
    )
}

@Composable
fun EmptyDecks(onCreateDeck: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "No hay mazos todavía",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Crea tu primer mazo para comenzar a estudiar",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        HButton(text = "Crear mazo", onClick = onCreateDeck, variant = ButtonVariant.Outline)
    }
}

@Preview(showSystemUi = true)
@PreviewLightDark
@Composable
fun DashboardScreenPreview() {
    HelloTheme {
        DashboardScreen(
            state = DashboardUiState(
                decks = listOf(
                    Deck(
                        id = "1",
                        name = "Inglés",
                        description = "Vocabulario",
                        createdAt = LocalDateTime.now(),
                        cards = listOf(),
                        cardsCount = 24
                    ),
                    Deck(
                        id = "2",
                        name = "Kotlin",
                        description = "Fundamentos",
                        createdAt = LocalDateTime.now(),
                        cards = listOf(),
                        cardsCount = 15
                    )
                )
            ),
            newCard = {},
            onDeckDetail = {},
            onCreateDeck = {}
        )
    }
}
package com.emm.hello.newfeatures.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
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
                        "Mis mazos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // FAB secundario: nuevo mazo
                SmallFloatingActionButton(
                    onClick = onCreateDeck,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = "Nuevo mazo")
                }
                // FAB principal: nueva tarjeta
                FloatingActionButton(
                    onClick = newCard,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva tarjeta")
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            // -- Session Summary ------------------------------------------------------
            item {
                val totalPending = state.decks.sumOf { it.cardsCount }
                AnimatedVisibility(
                    visible = !state.isLoading && totalPending > 0,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    SessionSummaryBanner(
                        totalDecks = state.decks.size,
                        totalCards = totalPending,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
            }

            // -- Section Header -------------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Mazos",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.decks.isNotEmpty()) {
                            HBadge(label = "${state.decks.size}", variant = BadgeVariant.Secondary)
                        }
                    }
                    HButton(
                        text = "Nuevo mazo",
                        onClick = onCreateDeck,
                        variant = ButtonVariant.Ghost,
                    )
                }
            }

            // -- Content --------------------------------------------------------------
            if (state.isLoading) {
                item { DashboardSkeleton(count = 4) }
            } else if (state.decks.isEmpty()) {
                item { EmptyDecks(onCreateDeck) }
            } else {
                itemsIndexed(state.decks, key = { _, deck -> deck.id }) { _, deck ->
                    DeckItem(deck = deck, onDeckClick = onDeckDetail)
                    HSeparator()
                }
                item { Spacer(modifier = Modifier.height(88.dp)) }
            }
        }
    }
}

// -- Component: Summary Banner ------------------------------------------------

@Composable
private fun SessionSummaryBanner(
    totalDecks: Int,
    totalCards: Long,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = "$totalCards tarjetas listas",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "en $totalDecks ${if (totalDecks == 1) "mazo" else "mazos"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Component: Deck Row ------------------------------------------------------

@Composable
private fun DeckItem(deck: Deck, onDeckClick: (String) -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                deck.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "${deck.cardsCount} tarjetas",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                // Barra de progreso visual (usa cardsCount como indicador)
                if (deck.cardsCount > 0) {
                    LinearProgressIndicator(
                        progress = { (deck.cardsCount % 10) / 10f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        },
        trailingContent = {
            HBadge(
                label = "${deck.cardsCount}",
                variant = BadgeVariant.Secondary,
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDeckClick(deck.id) },
    )
}

// -- Component: Empty State ---------------------------------------------------

@Composable
private fun EmptyDecks(onCreateDeck: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Sin mazos todavía",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Crea un mazo para empezar a estudiar",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            HButton(
                text = "Crear primer mazo",
                onClick = onCreateDeck,
                variant = ButtonVariant.Outline,
            )
        }
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(showSystemUi = true)
@PreviewLightDark
@Composable
private fun DashboardScreenPreview() {
    HelloTheme {
        DashboardScreen(
            state = DashboardUiState(
                decks = listOf(
                    Deck(
                        id = "1",
                        name = "Inglés B2",
                        description = "Vocabulario",
                        createdAt = LocalDateTime.now(),
                        cards = listOf(),
                        cardsCount = 24
                    ),
                    Deck(
                        id = "2",
                        name = "Phrasal Verbs",
                        description = "Verbos",
                        createdAt = LocalDateTime.now(),
                        cards = listOf(),
                        cardsCount = 7
                    ),
                    Deck(
                        id = "3",
                        name = "Gramática",
                        description = "Gramática",
                        createdAt = LocalDateTime.now(),
                        cards = listOf(),
                        cardsCount = 15
                    ),
                )
            ),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun DashboardScreenEmptyPreview() {
    HelloTheme {
        DashboardScreen(state = DashboardUiState(decks = emptyList()))
    }
}

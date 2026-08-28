package com.emm.hello.newfeatures.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.deck.Deck
import com.emm.domain.ids.toDeckId
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HEmptyState
import com.emm.hello.core.ui.HLoadingSpinner
import com.emm.hello.core.ui.HTopBar
import java.time.LocalDateTime

@Composable
fun DecksScreen(
    modifier: Modifier = Modifier,
    state: DecksUiState = DecksUiState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {},
    onIntent: (DecksUiIntent) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            HTopBar(
                onBack = onBack,
                title = stringResource(R.string.decks_title),
            )

            when {
                state.isLoading -> LoadingContent(modifier = Modifier.weight(1f))

                state.isEmpty -> HEmptyState(
                    modifier = Modifier.weight(1f),
                    headline = stringResource(R.string.decks_empty_headline),
                    body = stringResource(R.string.decks_empty_body),
                    primaryCta = {
                        HButton(
                            text = stringResource(R.string.decks_create_action),
                            onClick = { onIntent(DecksUiIntent.CreateDeckRequested) },
                            variant = HButtonVariant.Primary,
                            icon = Icons.Default.Add,
                        )
                    },
                )

                else -> DeckList(
                    modifier = Modifier.weight(1f),
                    decks = state.decks,
                    onDeckClick = { deckId -> onIntent(DecksUiIntent.DeckOpened(deckId)) },
                    onCreateDeck = { onIntent(DecksUiIntent.CreateDeckRequested) },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun DeckList(
    decks: List<Deck>,
    onDeckClick: (String) -> Unit,
    onCreateDeck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Spacer(Modifier.height(6.dp)) }

        items(decks, key = { it.id.value }) { deck ->
            DeckRow(deck = deck, onClick = { onDeckClick(deck.id.value) })
        }

        item {
            Spacer(Modifier.height(8.dp))
            HButton(
                text = stringResource(R.string.decks_create_action),
                onClick = onCreateDeck,
                variant = HButtonVariant.Secondary,
                icon = Icons.Default.Add,
                full = true,
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        HLoadingSpinner(size = 28.dp, color = inkMuted, strokeWidth = 2.dp)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090A)
@Composable
private fun DecksScreenPreview() {
    HelloTheme {
        Box(modifier = Modifier.fillMaxWidth()) {
            DecksScreen(
                state = DecksUiState(
                    isLoading = false,
                    decks = listOf(
                        previewDeck("deck-1", "Viajes", "Vocabulario para moverse"),
                        previewDeck("deck-2", "Trabajo", ""),
                    ),
                ),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090A)
@Composable
private fun DecksScreenEmptyPreview() {
    HelloTheme {
        Box(modifier = Modifier.fillMaxWidth()) {
            DecksScreen(state = DecksUiState(isLoading = false))
        }
    }
}

private val previewCreatedAt: LocalDateTime = LocalDateTime.parse("2026-01-01T00:00:00")

private fun previewDeck(id: String, name: String, description: String): Deck = Deck(
    id = id.toDeckId(),
    name = name,
    description = description,
    createdAt = previewCreatedAt,
    cards = emptyList(),
    cardsCount = 12L,
)

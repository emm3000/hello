package com.emm.hello.newfeatures.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.generation.LevelBand
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.HLoadingSpinner
import com.emm.hello.core.ui.HTopBar

@Composable
fun StoreScreen(
    modifier: Modifier = Modifier,
    state: StoreUiState = StoreUiState(),
    onIntent: (StoreUiIntent) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        HTopBar(
            onBack = { onIntent(StoreUiIntent.BackRequested) },
            title = stringResource(R.string.store_title),
        )

        if (state.isLoading) {
            LoadingContent(modifier = Modifier.weight(1f))
        } else {
            StoreList(
                modifier = Modifier.weight(1f),
                state = state,
                onIntent = onIntent,
            )
        }
    }
}

@Composable
private fun StoreList(
    state: StoreUiState,
    onIntent: (StoreUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Spacer(Modifier.height(6.dp)) }

        item { StoreHeader() }

        items(state.decks, key = { item -> item.id }) { item ->
            StoreDeckCard(
                item = item,
                isInstalling = state.installingDeckId == item.id,
                onInstall = { onIntent(StoreUiIntent.InstallRequested(item.id)) },
                onOpen = { onOpenDeck(item, onIntent) },
            )
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

private fun onOpenDeck(item: StoreDeckItem, onIntent: (StoreUiIntent) -> Unit) {
    val deckId: String = item.installedDeckId ?: return
    onIntent(StoreUiIntent.OpenDeckRequested(deckId))
}

@Composable
private fun StoreHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 14.dp),
    ) {
        Text(
            text = stringResource(R.string.store_headline),
            style = MaterialTheme.typography.headlineMedium,
            color = ink,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.store_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = inkMuted,
        )
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
private fun StoreScreenPreview() {
    HelloTheme {
        Box(modifier = Modifier.fillMaxWidth()) {
            StoreScreen(
                state = StoreUiState(
                    isLoading = false,
                    decks = listOf(
                        previewItem("traps", "Spanish traps", null),
                        previewItem("phrasal", "Everyday phrasal verbs", "curated-phrasal"),
                    ),
                ),
            )
        }
    }
}

private fun previewItem(id: String, name: String, installedDeckId: String?): StoreDeckItem = StoreDeckItem(
    id = id,
    name = name,
    description = "Words that trip up Spanish speakers.",
    tags = listOf("false friends", "daily life"),
    levelBand = LevelBand.B1_B2,
    cardsCount = 24,
    installedDeckId = installedDeckId,
)

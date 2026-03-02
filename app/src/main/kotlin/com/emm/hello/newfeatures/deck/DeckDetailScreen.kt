package com.emm.hello.newfeatures.deck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HSeparator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    modifier: Modifier = Modifier,
    deckName: String = "Vocabulario de Inglés",
    state: DeckDetailUiState = DeckDetailUiState(),
    onNavigateBack: () -> Unit = {},
    onReview: () -> Unit = {},
    onCardClick: (String) -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        deckName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                DeckDetailHeader(
                    cardCount = state.deck.cards.size,
                    onReview = onReview,
                    enabled = state.hasSessionEnabled,
                )
            }

            items(state.deck.cards, key = Flashcard::id) { card ->
                DeckCardItem(
                    card = card,
                    onCardClick = { onCardClick(it) }
                )
                HSeparator()
            }
        }
    }
}

@Composable
fun DeckDetailHeader(
    cardCount: Int,
    onReview: () -> Unit,
    enabled: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "$cardCount",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (cardCount == 1) "tarjeta" else "tarjetas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HButton(
            text = "Empezar repaso",
            onClick = onReview,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = enabled,
            leadingIcon = Icons.Filled.PlayArrow,
            variant = if (enabled) ButtonVariant.Default else ButtonVariant.Secondary,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Tus tarjetas",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (cardCount > 0) {
                HBadge(label = "$cardCount", variant = BadgeVariant.Secondary)
            }
        }
    }
}

@Composable
fun DeckCardItem(
    card: Flashcard,
    onCardClick: (String) -> Unit,
) {
    val reviewDate = Instant.ofEpochSecond(card.review.nextReviewAt)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    ListItem(
        headlineContent = {
            Text(
                text = card.word,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            val secondary = when {
                card.phonetic.isNotEmpty() -> card.phonetic
                card.meaning.isNotEmpty() -> card.meaning
                card.translation.isNotEmpty() -> card.translation
                else -> ""
            }
            if (secondary.isNotEmpty()) {
                Text(
                    text = secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = reviewDate.format(formatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick(card.id) }
    )
}

@PreviewLightDark
@Composable
fun DeckDetailScreenPreview() {
    HelloTheme {
        DeckDetailScreen(
            deckName = "Vocabulario Avanzado",
            state = DeckDetailUiState(
                deck = Deck.Empty.copy(
                    cards = listOf(
                        Flashcard(
                            id = "1",
                            word = "Serendipity",
                            meaning = "The occurrence of events by chance in a happy way",
                            translation = "Casualidad afortunada",
                            examples = listOf(
                                Example(
                                    exampleId = "1",
                                    text = "Finding that book was pure serendipity",
                                    translation = "Encontrar ese libro fue pura casualidad afortunada",
                                    type = "sentence"
                                )
                            ),
                            phonetic = "/ˌserənˈdɪpɪti/",
                            review = FlashcardReview.Empty
                        ),
                        Flashcard(
                            id = "2",
                            word = "Ephemeral",
                            meaning = "Lasting for a very short time",
                            translation = "Efímero",
                            examples = listOf(),
                            phonetic = "/ɪˈfem(ə)rəl/",
                            review = FlashcardReview.Empty
                        ),
                    )
                ),
                hasSessionEnabled = true
            )
        )
    }
}
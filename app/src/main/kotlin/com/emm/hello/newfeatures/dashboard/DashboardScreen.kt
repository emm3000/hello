package com.emm.hello.newfeatures.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.domain.deck.Deck
import com.emm.domain.deck.Tag
import com.emm.domain.ids.toDeckId
import com.emm.domain.study.DashboardStats
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberDivider
import com.emm.hello.core.theme.emberFaint
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberPrimary
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.instrumentSerif
import androidx.compose.ui.res.pluralStringResource
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonSize
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HCard
import com.emm.hello.core.ui.HChip
import com.emm.hello.core.ui.HEmptyState
import com.emm.hello.core.ui.HFab
import com.emm.hello.core.ui.HSearchBar
import com.emm.hello.core.ui.HSectionLabel
import java.time.LocalDateTime

// ─────────────────────────────────────────────────────────────────────────────
// DashboardScreen — top-level entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    state: DashboardUiState = DashboardUiState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    newCard: () -> Unit = {},
    onStudy: () -> Unit = {},
    onDeckDetail: (String) -> Unit = {},
    onCreateDeck: () -> Unit = {},
    onSettings: () -> Unit = {},
    onVisible: () -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onTagToggled: (String) -> Unit = {},
    onClearFilters: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        onVisible()
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Top row: wordmark + settings icon
            WordmarkRow(onSettings = onSettings)

            // Body: three states
            when {
                state.emptyState == DashboardEmptyState.LibraryEmpty -> {
                    EmptyLibraryContent(
                        modifier = Modifier.weight(1f),
                        onNewCard = newCard,
                        onCreateDeck = onCreateDeck,
                    )
                }

                state.emptyState == DashboardEmptyState.NoResults -> {
                    NoResultsContent(
                        modifier = Modifier.weight(1f),
                        state = state,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onTagToggled = onTagToggled,
                        onClearFilters = onClearFilters,
                        onNewCard = newCard,
                    )
                }

                else -> {
                    PopulatedContent(
                        modifier = Modifier.weight(1f),
                        state = state,
                        onDeckDetail = onDeckDetail,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onTagToggled = onTagToggled,
                        onClearFilters = onClearFilters,
                        onStudy = onStudy,
                    )
                }
            }
        }

        // FAB — always visible except LibraryEmpty (where CTA replaces it)
        if (state.emptyState != DashboardEmptyState.LibraryEmpty) {
            HFab(
                onClick = newCard,
                label = stringResource(R.string.dashboard_fab_label),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top wordmark row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WordmarkRow(
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // "Hello." — italic serif, accent period
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontFamily = instrumentSerif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 22.sp,
                        color = emberPrimary,
                        letterSpacing = (-0.2).sp,
                    ),
                ) {
                    append("Hello")
                }
                withStyle(
                    SpanStyle(
                        fontFamily = instrumentSerif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 22.sp,
                        color = emberAccent,
                        letterSpacing = (-0.2).sp,
                    ),
                ) {
                    append(".")
                }
            },
        )

        IconButton(onClick = onSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings_content_description),
                tint = emberMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Populated state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PopulatedContent(
    state: DashboardUiState,
    onDeckDetail: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onTagToggled: (String) -> Unit,
    onClearFilters: () -> Unit,
    onStudy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dueCount = state.stats?.cardsDueToday ?: 0
    val deckCount = state.totalDeckCount

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Hero block
        item {
            Spacer(Modifier.height(14.dp))
            HeroBlock(dueCount = dueCount)
            Spacer(Modifier.height(18.dp))
        }

        // CTA row: Estudiar ahora + N mazos
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HButton(
                    text = stringResource(R.string.dashboard_study_now),
                    onClick = onStudy,
                    variant = HButtonVariant.Accent,
                    size = HButtonSize.Md,
                    enabled = dueCount > 0,
                )
                Text(
                    text = pluralStringResource(R.plurals.dashboard_deck_count, deckCount, deckCount),
                    fontFamily = geistMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    letterSpacing = 0.06.em,
                    color = emberMuted,
                )
            }
            Spacer(Modifier.height(22.dp))
        }

        // Search bar
        item {
            HSearchBar(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = stringResource(R.string.dashboard_search_placeholder),
                clearContentDescription = stringResource(R.string.dashboard_search_clear_content_description),
                leadingIconContentDescription = stringResource(R.string.dashboard_search_icon_content_description),
            )
            Spacer(Modifier.height(14.dp))
        }

        // Tag chip row — horizontally scrollable
        if (state.availableTags.isNotEmpty()) {
            item {
                TagChipRow(
                    availableTags = state.availableTags,
                    selectedTags = state.selectedTags,
                    onTagToggled = onTagToggled,
                    onClearTags = onClearFilters,
                )
                Spacer(Modifier.height(26.dp))
            }
        }

        // Section label "Tus mazos" — no trailing action (domain has no per-deck due count)
        item {
            HSectionLabel(
                label = stringResource(R.string.dashboard_section_your_decks),
            )
            Spacer(Modifier.height(10.dp))
        }

        // Deck rows
        deckRows(state.decks, onDeckDetail)

        item { Spacer(Modifier.height(100.dp)) }
    }
}

private fun LazyListScope.deckRows(
    decks: List<Deck>,
    onDeckDetail: (String) -> Unit,
) {
    items(decks, key = { it.id.value }) { deck ->
        DeckRow(deck = deck, onClick = { onDeckDetail(deck.id.value) })
        Spacer(Modifier.height(10.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero block — editorial serif headline
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroBlock(dueCount: Int) {
    if (dueCount > 0) {
        // "{N} para repasar, el resto puede esperar."
        val part1 = "$dueCount "
        val part2 = stringResource(R.string.dashboard_hero_accent_part)
        val separator = stringResource(R.string.dashboard_hero_separator)
        val part3 = stringResource(R.string.dashboard_hero_muted_part)

        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontFamily = instrumentSerif,
                        fontSize = 46.sp,
                        color = emberPrimary,
                        letterSpacing = (-0.5).sp,
                    ),
                ) {
                    append(part1)
                }
                withStyle(
                    SpanStyle(
                        fontFamily = instrumentSerif,
                        fontSize = 46.sp,
                        color = emberAccent,
                        letterSpacing = (-0.5).sp,
                    ),
                ) {
                    append(part2)
                }
                withStyle(
                    SpanStyle(
                        fontFamily = instrumentSerif,
                        fontSize = 46.sp,
                        color = emberPrimary,
                        letterSpacing = (-0.5).sp,
                    ),
                ) {
                    append(separator)
                }
                withStyle(
                    SpanStyle(
                        fontFamily = instrumentSerif,
                        fontSize = 46.sp,
                        color = emberMuted,
                        letterSpacing = (-0.5).sp,
                    ),
                ) {
                    append("\n$part3")
                }
            },
            lineHeight = (46 * 1.04f).sp,
        )
    } else {
        // Zero due: calm alternative copy
        Text(
            text = stringResource(R.string.dashboard_hero_calm),
            fontFamily = instrumentSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 46.sp,
            lineHeight = (46 * 1.04f).sp,
            letterSpacing = (-0.5).sp,
            color = emberMuted,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tag chip row — horizontal scroll
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TagChipRow(
    availableTags: List<String>,
    selectedTags: Set<String>,
    onTagToggled: (String) -> Unit,
    onClearTags: (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // "todos" chip — active when no tags are selected
        val todosActive = selectedTags.isEmpty()
        HChip(
            label = stringResource(R.string.dashboard_chip_all),
            active = todosActive,
            onClick = { if (!todosActive) onClearTags?.invoke() },
        )
        availableTags.forEach { tag ->
            val isActive = selectedTags.contains(tag)
            HChip(
                label = tag,
                active = isActive,
                onClick = { onTagToggled(tag) },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DeckRow — card per deck in the list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun DeckRow(
    deck: Deck,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HCard(
        modifier = modifier.fillMaxWidth(),
        due = false, // domain model has no per-deck due count
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 20.dp),
        ) {
            // Title
            Text(
                text = deck.name,
                fontFamily = instrumentSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                color = emberPrimary,
                letterSpacing = (-0.2).sp,
                lineHeight = (22 * 1.1f).sp,
            )

            if (deck.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                // Description — italic serif muted
                Text(
                    text = deck.description,
                    fontFamily = instrumentSerif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = emberMuted,
                    lineHeight = (14 * 1.4f).sp,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Footer row: card count + divider + tags
            DeckRowFooter(deck = deck)
        }
    }
}

@Composable
private fun DeckRowFooter(deck: Deck) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Card count
        Text(
            text = stringResource(R.string.cards_count, deck.cardsCount),
            fontFamily = geistMono,
            fontWeight = FontWeight.Normal,
            fontSize = 10.5.sp,
            letterSpacing = 0.08.em,
            color = emberFaint,
        )

        if (deck.tags.isNotEmpty()) {
            // Thin divider line
            Canvas(
                modifier = Modifier
                    .width(14.dp)
                    .height(1.dp),
            ) {
                drawLine(
                    color = emberDivider,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            // Tags in mono muted
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                deck.tags.forEach { tag ->
                    Text(
                        text = tag.value,
                        fontFamily = geistMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.06.em,
                        color = emberMuted,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty library state (no decks yet)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyLibraryContent(
    onNewCard: () -> Unit,
    onCreateDeck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HEmptyState(
        modifier = modifier.fillMaxSize(),
        headline = stringResource(R.string.dashboard_empty_headline),
        accentWord = stringResource(R.string.dashboard_empty_headline_accent),
        body = stringResource(R.string.dashboard_empty_body),
        footnote = stringResource(R.string.dashboard_empty_footnote),
        primaryCta = {
            HButton(
                text = stringResource(R.string.dashboard_empty_cta_primary),
                onClick = onNewCard,
                variant = HButtonVariant.Accent,
                size = HButtonSize.Lg,
                full = true,
                icon = Icons.Outlined.AutoAwesome,
            )
        },
        ghostCta = {
            HButton(
                text = stringResource(R.string.dashboard_empty_cta_ghost),
                onClick = onCreateDeck,
                variant = HButtonVariant.Ghost,
                size = HButtonSize.Md,
                full = true,
            )
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// No-results state (search returned 0)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NoResultsContent(
    state: DashboardUiState,
    onSearchQueryChanged: (String) -> Unit,
    onTagToggled: (String) -> Unit,
    onClearFilters: () -> Unit,
    onNewCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Search bar (focused — it inherits focus styling from HSearchBar via state)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HSearchBar(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = stringResource(R.string.dashboard_search_placeholder),
                clearContentDescription = stringResource(R.string.dashboard_search_clear_content_description),
                leadingIconContentDescription = stringResource(R.string.dashboard_search_icon_content_description),
            )

            // Active filter chips
            if (state.availableTags.isNotEmpty()) {
                TagChipRow(
                    availableTags = state.availableTags,
                    selectedTags = state.selectedTags,
                    onTagToggled = onTagToggled,
                    onClearTags = onClearFilters,
                )
            }
        }

        // Centered editorial message
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                // Headline: Nada con "{query}".
                val query = state.searchQuery
                val headlineText = stringResource(R.string.dashboard_no_results_headline, "\"$query\"")
                val accentSpan = "\"$query\""
                Text(
                    text = buildAnnotatedString {
                        val before = headlineText.substringBefore(accentSpan)
                        val after = headlineText.substringAfter(accentSpan)
                        withStyle(
                            SpanStyle(
                                fontFamily = instrumentSerif,
                                fontSize = 36.sp,
                                color = emberPrimary,
                                letterSpacing = (-0.4).sp,
                            ),
                        ) {
                            append(before)
                        }
                        withStyle(
                            SpanStyle(
                                fontFamily = instrumentSerif,
                                fontStyle = FontStyle.Italic,
                                fontSize = 36.sp,
                                color = emberAccent,
                                letterSpacing = (-0.4).sp,
                            ),
                        ) {
                            append(accentSpan)
                        }
                        withStyle(
                            SpanStyle(
                                fontFamily = instrumentSerif,
                                fontSize = 36.sp,
                                color = emberPrimary,
                                letterSpacing = (-0.4).sp,
                            ),
                        ) {
                            append(after)
                        }
                    },
                    lineHeight = (36 * 1.1f).sp,
                )

                Spacer(Modifier.height(12.dp))

                // Body — recovery hint
                val activeTag = state.selectedTags.firstOrNull() ?: ""
                Text(
                    text = if (activeTag.isNotEmpty()) {
                        stringResource(R.string.dashboard_no_results_body_with_tag, activeTag)
                    } else {
                        stringResource(R.string.dashboard_no_results_body)
                    },
                    fontFamily = geist,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.5.sp,
                    lineHeight = (14.5f * 1.55f).sp,
                    color = emberMuted,
                )

                Spacer(Modifier.height(24.dp))

                // CTA: Crear con "{query}"
                HButton(
                    text = stringResource(R.string.dashboard_no_results_cta, "\"$query\""),
                    onClick = onNewCard,
                    variant = HButtonVariant.Accent,
                    size = HButtonSize.Md,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showSystemUi = true)
@Composable
private fun DashboardScreenPreview() {
    HelloTheme {
        DashboardScreen(
            state = DashboardUiState(
                isLoading = false,
                totalDeckCount = 3,
                stats = DashboardStats(
                    cardsStudiedToday = 0,
                    cardsDueToday = 12,
                    currentStreak = 3,
                    cardsDueThisWeek = 24,
                ),
                decks = listOf(
                    Deck(
                        id = "1".toDeckId(),
                        name = "Trabajo & carrera",
                        description = "palabras de oficina, reuniones, email",
                        createdAt = LocalDateTime.now(),
                        cards = listOf(),
                        cardsCount = 38,
                        tags = listOf(Tag("trabajo"), Tag("formal")),
                    ),
                    Deck(
                        id = "2".toDeckId(),
                        name = "Phrasal verbs",
                        description = "los que se confunden todo el tiempo",
                        createdAt = LocalDateTime.now(),
                        cards = listOf(),
                        cardsCount = 64,
                        tags = listOf(Tag("gramática")),
                    ),
                    Deck(
                        id = "3".toDeckId(),
                        name = "Aeropuerto & viajes",
                        description = "check-in, conexiones, equipaje",
                        createdAt = LocalDateTime.now(),
                        cards = listOf(),
                        cardsCount = 18,
                        tags = listOf(Tag("viaje")),
                    ),
                ),
                availableTags = listOf("formal", "gramática", "trabajo", "viaje"),
            ),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun DashboardScreenEmptyPreview() {
    HelloTheme {
        DashboardScreen(
            state = DashboardUiState(
                isLoading = false,
                decks = emptyList(),
                emptyState = DashboardEmptyState.LibraryEmpty,
            ),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun DashboardScreenNoResultsPreview() {
    HelloTheme {
        DashboardScreen(
            state = DashboardUiState(
                isLoading = false,
                decks = emptyList(),
                totalDeckCount = 3,
                searchQuery = "negociar",
                selectedTags = setOf("formal"),
                availableTags = listOf("formal", "trabajo", "viaje"),
                emptyState = DashboardEmptyState.NoResults,
            ),
        )
    }
}

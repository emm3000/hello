# Design: Card Search in Deck Detail

## Technical Approach

Add in-memory card filtering to `DeckDetailScreen` driven by MVI intent. The ViewModel holds a `searchQuery` in state; the composable derives a filtered card list from `state.deck.cards`. No new dependencies, no data-layer changes — pure app-layer addition.

## Architecture Decisions

| Decision | Options | Tradeoff | Choice |
|----------|---------|----------|--------|
| Where filtering happens | Composable-derived vs ViewModel-computed | Composable-derived = no extra state, recomputes on every recomposition. ViewModel = cached, but extra boilerplate for derived state | **Composable-derived** — filtering is O(n) on small lists (<500 cards), no perf concern. Keeps ViewModel lean |
| Filter scope | word only vs word+translation+meaning | word-only = fast but misses cards user finds by translation. All 3 fields = covers all search intents | **word + translation + meaning** — matches how users think about cards |
| Match type | prefix vs contains vs exact | prefix = faster, less noise. contains = finds more results, slightly slower. exact = too restrictive | **contains, case-insensitive** — best recall for flashcard search |
| TextField placement | TopAppBar vs LazyColumn header | TopAppBar = always visible but reduces title space. LazyColumn header = scrolls with content, natural placement | **LazyColumn header, pinned below TopAppBar** — scrolls with content, uses `stickyHeader` so it stays visible |
| Empty state | Reuse `EmptyCards` vs new `EmptySearchResults` | Reusing = confusing (implies deck has no cards at all). Separate = clear distinction | **New `EmptySearchResults` composable** — distinct message: "No cards match your search" |

## Data Flow

```
User types in TextField
    │
    ▼
onIntent(SearchCardsChanged("hello"))
    │
DeckDetailViewModel.onIntent()
    │ mutableState.value = state.copy(searchQuery = query)
    ▼
DeckDetailUiState(searchQuery = "hello")
    │
DeckDetailScreen (composable)
    │ val filteredCards = state.deck.cards.filter { matchesQuery(it, state.searchQuery) }
    ▼
LazyColumn renders filteredCards
    ├─ stickyHeader: SearchBar(query, onQueryChange, onClear)
    ├─ SectionBlock: "Tarjetas" + badge "3 / 47"
    └─ items(filteredCards): DeckCardItem per card
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `app/.../deck/DeckDetailUiIntent.kt` | Modify | Add `data class SearchCardsChanged(val query: String) : DeckDetailUiIntent` |
| `app/.../deck/DeckDetailUiState.kt` | Modify | Add `val searchQuery: String = ""` |
| `app/.../deck/DeckDetailViewModel.kt` | Modify | Handle `SearchCardsChanged` intent → update `searchQuery` in state |
| `app/.../deck/DeckDetailScreen.kt` | Modify | Add `stickyHeader` with search TextField, filtered list logic, `EmptySearchResults`, filtered count badge |
| `app/src/main/res/values/strings.xml` | Modify | Add 4 string resources for search UI |

## Interfaces / Contracts

### Intent

```kotlin
sealed interface DeckDetailUiIntent {
    data class SearchCardsChanged(val query: String) : DeckDetailUiIntent
}
```

### State

```kotlin
data class DeckDetailUiState(
    val deck: Deck = Deck.empty(SystemClock),
    val hasSessionEnabled: Boolean = false,
    val searchQuery: String = "",
)
```

### Filter Function

```kotlin
/**
 * Returns true if the flashcard matches the search query.
 * Matches against word, translation, and meaning (case-insensitive, trimmed).
 * Empty query always returns true (no filter).
 */
internal fun Flashcard.matchesSearchQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val normalizedQuery = query.trim().lowercase()
    return word.lowercase().contains(normalizedQuery) ||
        translation.lowercase().contains(normalizedQuery) ||
        meaning.lowercase().contains(normalizedQuery)
}
```

**Edge cases:**
- Empty/whitespace query → no filter, shows all cards
- Single character → matches any field containing that character
- Query longer than any field → no match (correct behavior)
- Fields that are blank strings → `"".contains(x)` = false, safe

### ViewModel Intent Handler

```kotlin
override fun onIntent(intent: DeckDetailUiIntent) {
    when (intent) {
        is DeckDetailUiIntent.SearchCardsChanged -> {
            mutableState.value = mutableState.value.copy(searchQuery = intent.query)
        }
    }
}
```

## UI Composition

```
DeckDetailScreen (Scaffold)
├── TopAppBar                          ← existing (deck name, back button)
└── LazyColumn
    ├── stickyHeader {                 ← NEW
    │       SearchTextField(
    │           query = state.searchQuery,
    │           onQueryChange = { onIntent(SearchCardsChanged(it)) },
    │           onClear = { onIntent(SearchCardsChanged("")) }
    │       )
    │   }
    ├── DeckStatsHeader                ← existing (unchanged, uses ALL cards)
    ├── if (hasQuery && filteredCards.isEmpty())
    │       EmptySearchResults         ← NEW: icon + "No se encontraron tarjetas"
    ├── if (filteredCards.isNotEmpty())
    │       SectionBlock(
    │           title = "Tarjetas",
    │           trailing = HBadge("${filteredCards.size} / ${allCards.size}")
    │       )
    │       items(filteredCards) { DeckCardItem }
    └── if (noQuery && allCards.isEmpty())
            EmptyCards                 ← existing (unchanged)
```

**SearchTextField spec:**
- `OutlinedTextField` with leading search icon (`Icons.Default.Search`)
- Trailing clear icon (`Icons.Default.Close`) visible only when query is non-empty
- Placeholder: `"Buscar tarjetas..."`
- `singleLine = true`
- `modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.xs)`
- Background: `MaterialTheme.colorScheme.surface`

**EmptySearchResults spec:**
- Centered icon (`Icons.Outlined.Search`) at 48dp
- Title: `"Sin resultados"`
- Description: `"No se encontraron tarjetas que coincidan con tu búsqueda"`
- Wrapped in `SectionBlock` for visual consistency with `EmptyCards`

## Route Wiring

`DeckDetailRoute` passes `onIntent` to `DeckDetailScreen`:

```kotlin
DeckDetailScreen(
    state = uiState,
    onIntent = vm::onIntent,          // NEW parameter
    onNavigateBack = { navController.popBackStack() },
    onReview = { navController.navigate(StudyRoute(uiState.deck.id.value)) },
    onCardClick = { cardId -> navController.navigate(CardDetailRoute(cardId)) },
    onAddCard = { navController.navigate(NewCardRoute) },
)
```

## String Resources

```xml
<!-- Deck Detail Screen - Search -->
<string name="search_cards_placeholder">Buscar tarjetas...</string>
<string name="clear_search">Limpiar búsqueda</string>
<string name="no_search_results">Sin resultados</string>
<string name="no_search_results_description">No se encontraron tarjetas que coincidan con tu búsqueda</string>
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit — Filter | `matchesSearchQuery` with empty query, single char, multi-word, case variations, blank fields | Pure function tests, no coroutines |
| Unit — ViewModel | `SearchCardsChanged` intent updates `searchQuery` in state, other state fields preserved | `Turbine` on `uiState`, fake use cases |
| Composable | SearchTextField renders, clear button appears/disappears correctly, filtered list updates | Compose UI tests with `createComposeRule` |

## Migration / Rollout

No migration needed. Fully additive change — no existing behavior modified. Rollback = revert commits.

## Open Questions

- None

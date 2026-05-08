# Tasks: Card Search in Deck Detail

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~130–155 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | MVI scaffolding + filter logic + tests | PR 1 | Domain + ViewModel + pure function + unit tests |
| 2 | UI wiring + string resources | PR 2 | Composable changes, search bar, empty state, badge |

## Phase 1: MVI Scaffolding

- [ ] 1.1 Add `data class SearchCardsChanged(val query: String) : DeckDetailUiIntent` to `app/.../deck/DeckDetailUiIntent.kt`
- [ ] 1.2 Add `val searchQuery: String = ""` to `DeckDetailUiState` data class in `app/.../deck/DeckDetailUiState.kt`
- [ ] 1.3 Add `is DeckDetailUiIntent.SearchCardsChanged` branch in `DeckDetailViewModel.onIntent()` → `mutableState.value = state.copy(searchQuery = intent.query)` in `app/.../deck/DeckDetailViewModel.kt`

## Phase 2: Filter Logic

- [ ] 2.1 Create `internal fun Flashcard.matchesSearchQuery(query: String): Boolean` extension — trim + lowercase query, check word/translation/meaning with `contains`, return true for blank query. Place in `app/.../deck/DeckDetailScreen.kt` (private to file) or new `app/.../deck/CardSearchFilter.kt`
- [ ] 2.2 Write unit tests for `matchesSearchQuery()` covering: empty query → true, whitespace-only → true, case-insensitive match on word, match on translation, match on meaning, no match → false, special chars in query. File: `app/src/test/.../deck/CardSearchFilterTest.kt`

## Phase 3: UI Wiring

- [ ] 3.1 Add 4 string resources to `app/src/main/res/values/strings.xml`: `search_cards_placeholder`, `clear_search`, `no_search_results`, `no_search_results_description`
- [ ] 3.2 Add `onIntent: (DeckDetailUiIntent) -> Unit` parameter to `DeckDetailScreen` composable signature (if not already present) and wire in `DeckDetailRoute`
- [ ] 3.3 Add `stickyHeader` with `OutlinedTextField` (search icon, clear button, placeholder) to `LazyColumn` in `DeckDetailScreen.kt` — query binds to `state.searchQuery`, emits `SearchCardsChanged` on change
- [ ] 3.4 Derive `filteredCards` in composable via `state.deck.cards.filter { it.matchesSearchQuery(state.searchQuery) }`; update `SectionBlock` trailing badge to show `"${filteredCards.size} / ${allCards.size}"`
- [ ] 3.5 Add conditional `EmptySearchResults` composable (icon + title + description) when `state.searchQuery.isNotBlank() && filteredCards.isEmpty()`, placed before the SectionBlock; ensure existing `EmptyCards` shows only when no query and deck is empty

## Phase 4: Verification

- [ ] 4.1 Add ViewModel test: `SearchCardsChanged` intent updates `searchQuery` in emitted state, other state fields preserved. Extend `app/src/test/.../deck/DeckDetailViewModelTest.kt`
- [ ] 4.2 Run detekt and verify 0 issues
- [ ] 4.3 Run all tests and verify pass

# Current Dashboard

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Dashboard` flow |
| Source of Truth | No |
| Read this when | You need to understand the current deck list, search and filters |

## Summary

`Dashboard` is the main screen for viewing decks, navigating to detail, creating deck/card and applying search + tag filters over local data from `HelloDb`.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/shared/UndoEventHolder.kt` (shared singleton — also used by deck/card flows)
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardStatsSection.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardUiEffect.kt`

## State and criteria

`DashboardUiState` consolidates into a single source:

- `decks` (rendered list)
- `allDecks` (unfiltered list, refreshed on every emission of the combine flow; source for `totalDeckCount` and `availableTags`)
- `totalDeckCount`
- `isLoading`
- `searchQuery`
- `selectedTags`
- `availableTags`
- `isFiltering`
- `emptyState` (`LibraryEmpty`, `NoResults`, `None`)
- `stats` (`DashboardStats?` — cards studied/due today, current streak, cards due this week)

The rendered list is computed from active criteria (query + tags), not from parallel sources.

`allDecks` is a public field of `DashboardUiState`, not a private viewmodel list: `buildState(allDecks, filteredDecks)` writes both lists into the same state on every emission.

## Intents

- `QueryChanged(value)` — updates `searchQuery` and feeds the search criteria flow.
- `TagToggled(tag)` — normalizes the tag (`trim().lowercase()`) and toggles it in `selectedTags`.
- `ClearFilters` — clears `searchQuery` + `selectedTags` and resets criteria to defaults.
- `ScreenVisible` — loads `DashboardStats` via `GetDashboardStatsUseCase`.
- `StudyClicked` — emits `NavigateToStudy(StudyRoute.ALL_DUE_DECKS)`. The global CTA keys off `stats.cardsDueToday` (across all decks), so it studies every card due today instead of an arbitrary first deck; `StudyViewModel` resolves the sentinel to the all-decks session.
- `UndoDeleteDeck(deckId, deletedAt)` — calls `RestoreDeckUseCase` to reverse a soft-delete using the cascade timestamp.

## Effects

`DashboardUiEffect`:

- `NavigateToStudy(deckId: String)` — collected in `DashboardDestination` via `LaunchedEffect(Unit)`; navigates to `StudyRoute(deckId)`.
- `ShowUndoDeckDeleted(deckName, deckId, deletedAt)` — produced when `DashboardViewModel` receives a `UndoEvent.DeckDeleted` from `UndoEventHolder`; triggers a `SnackbarHost` snackbar ("Mazo X eliminado" + "Deshacer" action) in `DashboardScreen`.

`DashboardRoute` wires `onStudy = { vm.onIntent(StudyClicked) }` and passes it down to `DashboardScreen`, which forwards it to the stats section's "Estudiar ahora" CTA. Previously that CTA called `newCard` directly (navigating to `NewCardRoute`).

## Undo delete deck

`DashboardViewModel` collects `UndoEvent.DeckDeleted` events from the app-level `UndoEventHolder` singleton (a `MutableSharedFlow` with `extraBufferCapacity = 1`, no replay) and converts them into `ShowUndoDeckDeleted` effects. `DashboardScreen` renders a `SnackbarHost`; tapping "Deshacer" dispatches `UndoDeleteDeck(deckId, deletedAt)`, which calls `RestoreDeckUseCase`.

Stats loading goes through the normal MVI channel: `DashboardRoute` wires the screen's `onVisible` callback to `vm.onIntent(ScreenVisible)` (the viewmodel no longer exposes an `onVisible()` method), and the viewmodel loads `DashboardStats` via `GetDashboardStatsUseCase`.

## Search and filters

- search by deck name, case-insensitive
- tag filters with intersection (match ALL)
- criteria flow is debounced (~120 ms) and de-duplicated before hitting `GetFilteredDecksUseCase`
- `ClearFilters` action clears query + tags in a single step

## Filter persistence decision

Filters are currently **not persisted** across sessions.

- on open/recreate, `searchQuery` starts empty
- `selectedTags` starts empty

Reason: keep behavior predictable and avoid stale state across sessions while the product remains local-first single-device.

## Empty states

- `LibraryEmpty`: no decks in local database
- `NoResults`: decks exist, but no results match the active criteria

## Reused UI components

The UI uses shared components from `core/ui` (Ember dark redesign, Phase 2.1):

- `HSearchBar`
- `HChip` (tag chips, including the `todos` chip)
- `HButton` (`Accent` / `Secondary` / `Ghost`)
- `HCard` (deck rows)
- `HFab` (primary "New card" entry point)
- `HEmptyState` (`LibraryEmpty` editorial state)
- `HSectionLabel` ("Tus mazos")
- `HStatCard` (inside `DashboardStatsSection`)

The screen renders three top-level branches driven by `state.emptyState`: `EmptyLibraryContent` (`LibraryEmpty`), `NoResultsContent` (`NoResults`), and `PopulatedContent` (default). The FAB is hidden in the `LibraryEmpty` branch since the empty state provides its own CTA. Raw Material3 is only used for `Icon`/`IconButton` chrome (settings gear in the wordmark row).

The root `Column` applies `statusBarsPadding()` so the wordmark/settings gear row is never occluded by the status bar.

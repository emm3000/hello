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
- `totalDeckCount`
- `isLoading`
- `searchQuery`
- `selectedTags`
- `availableTags`
- `isFiltering`
- `emptyState` (`LibraryEmpty`, `NoResults`, `None`)
- `stats` (`DashboardStats?` — cards studied/due today, current streak, cards due this week)

The rendered list is computed from active criteria (query + tags), not from parallel sources.

`DashboardViewModel` also maintains a private `allDecks` list (updated on every emission of the combine flow) to support the `StudyClicked` handler without requiring due-count data in state.

## Intents

- `QueryChanged(value)` — updates `searchQuery` and feeds the search criteria flow.
- `TagToggled(tag)` — normalizes the tag (`trim().lowercase()`) and toggles it in `selectedTags`.
- `ClearFilters` — clears `searchQuery` + `selectedTags` and resets criteria to defaults.
- `StudyClicked` — picks the first deck from `allDecks` and emits `NavigateToStudy(deckId)`; no-op if the list is empty.

## Effects

`DashboardUiEffect`:

- `NavigateToStudy(deckId: String)` — collected in `DashboardDestination` via `LaunchedEffect(Unit)`; navigates to `StudyRoute(deckId)`.

`DashboardRoute` wires `onStudy = { vm.onIntent(StudyClicked) }` and passes it down to `DashboardScreen`, which forwards it to the stats section's "Estudiar ahora" CTA. Previously that CTA called `newCard` directly (navigating to `NewCardRoute`).

The `ViewModel` also exposes `onVisible()`, which the screen invokes via a `LaunchedEffect(Unit)` to load `DashboardStats` via `GetDashboardStatsUseCase` on entry.

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

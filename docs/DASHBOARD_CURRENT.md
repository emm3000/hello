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
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardUiIntent.kt`

## State and criteria

`DashboardUiState` consolidates into a single source:

- `searchQuery`
- `selectedTags`
- `availableTags`
- `decks` (rendered list)
- `totalDeckCount`
- `emptyState` (`LibraryEmpty`, `NoResults`, `None`)

The rendered list is computed from active criteria (query + tags), not from parallel sources.

## Search and filters

- search by deck name, case-insensitive
- tag filters with intersection (match ALL)
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

The UI uses shared components from `core/ui`:

- `HSearchBar`
- `HTagChip`
- `HButton`
- `HBadge`

No raw Material3 components are introduced for search/filter controls.

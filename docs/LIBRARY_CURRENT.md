# Current Library

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Library` flow (all cards, content search) |
| Source of Truth | No |
| Read this when | You need to understand how cards are listed, searched and filtered |
| Last verified | 2026-08-28 |

## Summary

`Library` lists every alive flashcard and matches a search against the
card's own content rather than against the deck that holds it. It is the
densest of the three surfaces — `docs/DESIGN_BRIEF.md` calls it the one
screen where Notion-style density is appropriate, because it is a browsing
surface and not a focus surface.

It replaced the dashboard deck list and Deck Detail. Reached from the
"Library" `HButton` on Today (`TodayRoute` navigates to `LibraryRoute`).

## Key files

### Domain

- `domain/src/main/kotlin/com/emm/domain/library/LibraryFlashcard.kt`
- `domain/src/main/kotlin/com/emm/domain/library/LibraryRepository.kt`
- `domain/src/main/kotlin/com/emm/domain/library/SearchLibraryUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/text/SearchText.kt`

### Data

- `data/src/main/sqldelight/com/emm/data/Flashcard.sq` — `libraryFlashcards`
- `data/src/main/kotlin/com/emm/data/library/DefaultLibraryRepository.kt`

### App

- `app/src/main/kotlin/com/emm/hello/newfeatures/library/LibraryRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/library/LibraryScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/library/LibraryViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/library/LibraryUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/library/LibraryUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/library/LibraryUiEffect.kt`

## Read model

`LibraryFlashcard` carries `id`, `deckId`, `deckName`, `word`, `translation`,
`meaning`, `enrichmentStatus` and a nullable `nextReviewAt`.

`libraryFlashcards` inner-joins `Deck` for the name and left-joins
`ReviewProjection` for `nextReviewAt`, which stays null for a card that has
never been reviewed. It selects every card with `deletedAt IS NULL`; a deck
soft delete already cascades to its cards, so that is the only predicate.
Order is `createdAt DESC, id`.

`translation` is a nullable column and maps to an empty string. The raw
`enrichmentStatus` goes through `toEnrichmentStatus`, the same decoder the
other flashcard queries use.

## Search policy

`SearchLibraryUseCase(query, deckId?)` owns the matching and lives in
`:domain`, so it is testable without a database.

- The deck filter narrows first, then the query matches.
- A blank query returns everything the filter left.
- A card matches when the needle appears in `word`, `translation` or `meaning`.
- `searchNormalized` decomposes to NFD, drops combining marks and lowercases
  with `Locale.ROOT`, on both the query and the field. `cafe` finds `café` and
  `CAFÉ` finds it too.
- The repository's ordering is preserved.

Nine tests in `SearchLibraryUseCaseTest` cover blank input, case, diacritics
on either side, meaning matches, the deck filter, their combination, a miss
and the ordering.

## State

`LibraryUiState`: `cards`, `decks`, `query`, `selectedDeckId`, `isLoading`,
`referenceNow: Instant`.

`referenceNow` is refreshed from the injected `Clock` on every search
emission, so the schedule labels in the rows are computed against the moment
the list was last produced, not against composition time.

Three computed flags:

- `isFiltered` — a non-blank query or a selected deck
- `isLibraryEmpty` — loaded, no cards, not filtered
- `hasNoResults` — loaded, no cards, filtered

The empty-library and no-results states are told apart from the filter alone.
No second count is asked of the database.

`LibraryUiState.kt` also declares `ScheduleStatus` (`New`, `DueToday`,
`InDays(days)`) and `LibraryFlashcard.scheduleStatus(now, zone)`: a null
`nextReviewAt` is `New`; otherwise the day difference between `now` and the
review instant in `zone` is `DueToday` when `<= 0`, else `InDays`.

## Intents

- `QueryChanged(value)` — updates state and the criteria flow.
- `DeckFilterToggled(deckId)` — selects a deck, or clears it when it is
  already selected.
- `FiltersCleared` — resets query and deck.
- `CardOpened(card)` — emits `OpenCard`.
- `CaptureRequested` — emits `OpenCapture`.
- `UndoDeleteCard(flashcardId, deletedAt)` — calls `RestoreFlashcardUseCase`.

The criteria flow is debounced 200 ms and de-duplicated before
`flatMapLatest` re-subscribes the search.

## Effects

`LibraryUiEffect`:

- `OpenCard(cardId, deckId)` → `CardDetailRoute`
- `OpenCapture` → `CaptureRoute`
- `ShowUndoCardDeleted(flashcardId, deletedAt)` — produced when the ViewModel
  receives `UndoEvent.CardDeleted` from `UndoEventHolder`; raises the
  "Card deleted" snackbar with an "Undo" action.
- `ShowMessage(messageRes)` — a Toast, used when a restore fails.

Library is where a card deletion lands after `FlashcardDetailViewModel`
navigates back, so it owns the card-deleted undo that Deck Detail used to own.

## Layout

`HTopBar` with a back arrow, the title "Library", and the visible card count
("N words") in `inkMuted`. Below it a persistent `HSearchBar`, then a
horizontally scrolling `HChip` row — an "All" chip first, then one chip per
deck — rendered only when there is more than one deck, since a single deck is
not a choice.

A row is two lines: the word at 17 sp semibold, the translation at 14 sp
`inkMuted` (omitted when blank). The right edge carries one 12 sp marker —
"Preparing…" in `warningInk` for `PENDING`, "Failed" in `destructiveInk` for
`FAILED`, otherwise the live schedule label in `inkMuted`: "new", "due today"
or "in N days", from `scheduleStatus(referenceNow, ZoneId.systemDefault())`.
The deck name is not shown on the row. Rows are separated by `HSeparator`,
not by cards.

Loading shows a centered `HLoadingSpinner`; the empty library and the
no-results case are `HEmptyState`s whose CTAs are an `HButton`
("Add a word" → `CaptureRequested`, "Clear filters" → `FiltersCleared`).

## Persistence

Read-only apart from the undo restore. Everything comes from `HelloDb`.

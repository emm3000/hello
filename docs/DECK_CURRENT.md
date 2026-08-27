# Current Decks

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Mazos` and `New/Edit Deck` flows |
| Source of Truth | No |
| Read this when | You need to understand deck creation, editing and deletion |
| Last verified | 2026-08-27 |

## Summary

A deck is an optional grouping, not the axis the app is organised around, so
deck management lives in Settings → Organización → Mazos rather than on the
daily path.

Two sibling flows on the `deck` feature:

- `Mazos` lists the decks and opens the form.
- `New/Edit Deck` creates, renames and deletes, one screen and one viewmodel,
  distinguished by `DeckFormMode`.

`Deck Detail` no longer exists. A deck's card list is `Biblioteca` filtered by
that deck's chip — see `LIBRARY_CURRENT.md`.

## Key files

### Mazos

- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DecksRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DecksScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DecksViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DecksUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DecksUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DecksUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckRow.kt`

### New / Edit Deck

- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckFormMode.kt` (`Create` / `Edit(deckId)`)

## Mazos

### State

`DecksUiState`: `decks`, `isLoading`, and a computed `isEmpty` (loaded with
no decks).

### Loading

`GetDecksUseCase()` streams `deckWithFlashcardCount`; the first emission
clears `isLoading`.

### Intents

- `DeckOpened(deckId)` → emits `OpenDeckForm(deckId)`
- `CreateDeckRequested` → emits `OpenDeckForm(null)`
- `UndoDeleteDeck(deckId, deletedAt)` → `RestoreDeckUseCase`

### Effects

`DecksUiEffect`:

- `OpenDeckForm(deckId?)` → `NewDeckRoute(deckId)`
- `ShowUndoDeckDeleted(deckName, deckId, deletedAt)` — produced when the
  ViewModel receives `UndoEvent.DeckDeleted` from `UndoEventHolder`; raises
  the "Mazo X eliminado" snackbar with a "Deshacer" action
- `ShowMessage(messageRes)` — a Toast, used when a restore fails

### Layout

`HTopBar` with the title, a `DeckRow` per deck (name, description, card count
and tags in mono), and a secondary "Nuevo mazo" button at the end of the
list. The empty state offers the same action as its primary CTA.

## New / Edit Deck

### State

`NewDeckUiState`:

- `name`, `description`, `tags: List<String>` (normalized: lowercase + trim +
  distinct + non-blank)
- `isLoading`
- `formMode: DeckFormMode`
- `isDeleteConfirmationVisible`
- `isValid` (computed): `name` not blank
- `canDelete` (computed): edit mode and not loading

### Loading

Only in `DeckFormMode.Edit`: `DeckRepository.fetchById(deckId).first()`
populates `name`, `description` and `tags`.

### Intents

- `NameChanged`, `DescriptionChanged`, `TagsChanged` (normalizes before state)
- `Submit` — short-circuits on `!isValid || isLoading`
- `DeleteDeck` — opens the confirmation
- `ConfirmDeleteDeck` — soft deletes
- `DismissDeleteDeck` — closes the confirmation

### Submit

- `Create` → `DeckRepository.create(CreateDeckInput(...))` → reset + `NavigateBack`
- `Edit` → `UpdateDeckUseCase(UpdateDeckInput(...))` → `NavigateBack`

### Delete

`ConfirmDeleteDeck` returns early unless `formMode is DeckFormMode.Edit`, so a
confirm in create mode never reaches the repository. Otherwise it calls
`SoftDeleteDeckUseCase`, emits `UndoEvent.DeckDeleted` to `UndoEventHolder`
with the returned timestamp, and emits `DeckDeleted` so the route navigates
back to Mazos, where the undo snackbar is waiting.

`Deck.sq` cascades the soft delete to the deck's flashcards and their
examples, so the deleted deck's cards leave Biblioteca and the study session
with it.

The affordance is a danger ghost button at the bottom of the form plus an
`HAlertDialog` confirmation — the same shape the card editor uses for
"Borrar tarjeta".

### Effects

`NewDeckUiEffect`: `NavigateBack`, `DeckDeleted`, `ShowMessage(messageRes)`.

## Persistence

- Read/write: 100% local on `HelloDb` via repos and use cases from
  `:domain` / `:data`.
- Soft delete preserves data and respects `LOCAL_FIRST.md`.

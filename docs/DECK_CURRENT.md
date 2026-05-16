# Current Decks

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Deck Detail` and `New/Edit Deck` flows |
| Source of Truth | No |
| Read this when | You need to understand deck creation, editing and detail view |

## Summary

Two sibling flows on the same `deck` feature:

- `Deck Detail` shows deck info, its card list with local search, and entry points to edit/delete.
- `New/Edit Deck` reuses the same screen and viewmodel for creating or editing a deck, distinguished by `DeckFormMode`.

## Key files

### Deck Detail

- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailUiEffect.kt`

### New / Edit Deck

- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckFormMode.kt` (`Create` / `Edit(deckId)`)

## Deck Detail

### State

`DeckDetailUiState`:

- `deck: Deck` (empty default with `SystemClock`)
- `hasSessionEnabled` (true if any card has `nextReviewAt <= now`)
- `searchQuery`
- `showDeleteConfirmation`

### Loading

`DeckDetailViewModel.init` combines two flows:

- `GetDeckDetailUseCase(deckId)` — deck info + cards
- `ObserveFlashcardsWithReviewUseCase(deckId)` — flashcards with review schedule

The merge (`mergeDeckCardsById`) overwrites the `review` field of the deck cards with the one from the study flow, keeping the rest of the fields.

### Actions

- `SearchCardsChanged(query)` — updates local filter (matching by `word`, `translation`, `meaning`, case-insensitive)
- `EditDeck` → emits `NavigateToEditDeck(deckId)`
- `DeleteDeck` → opens confirmation
- `ConfirmDeleteDeck` → `SoftDeleteDeckUseCase` + emits `DeckDeleted`
- `DismissDeleteDeck` → closes confirmation

### Effects

`DeckDetailUiEffect`:

- `NavigateToEditDeck(deckId)`
- `DeckDeleted`
- `ShowMessage(text)`

## New / Edit Deck

### State

`NewDeckUiState`:

- `name`, `description`, `tags: List<String>` (normalized: lowercase + trim + distinct + non-blank)
- `isLoading`
- `formMode: DeckFormMode` (`Create` or `Edit(deckId)`)
- `isValid` (computed): `name` not empty

### Loading

Only if `formMode is DeckFormMode.Edit`:

- `DeckRepository.findById(deckId).first()`
- populates `name`, `description`, `tags` from the loaded deck

### Actions

Intents:

- `NameChanged(name)`
- `DescriptionChanged(description)`
- `TagsChanged(tags)` — normalizes before saving to state
- `Submit` — short-circuits if `!isValid || isLoading`

### Submit

- `DeckFormMode.Create` → `DeckRepository.addDeck(CreateDeckInput(...))` → reset state + `NavigateBack`
- `DeckFormMode.Edit` → `UpdateDeckUseCase(UpdateDeckInput(...))` → `NavigateBack`

### Effects

`NewDeckUiEffect`:

- `NavigateBack`
- `ShowMessage(text)`

## Persistence

- Read/write: 100% local on `HelloDb` via repos and use cases from the `:domain`/`:data` modules.
- Soft delete preserves data and respects `LOCAL_FIRST.md`.

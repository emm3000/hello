# Current Card Detail

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Card Detail` flow |
| Source of Truth | No |
| Read this when | You need to understand how an existing card is shown and deleted |

## Summary

`Card Detail` shows a saved flashcard and lets you go to edit or delete it (soft delete). It opens from the deck detail or from the dashboard.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/card/CardDetailRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiEffect.kt`

## State

`FlashcardDetailUiState` holds:

- `flashcard: FlashcardDetail` (empty default with `SystemClock`)
- `showDeleteConfirmation`

## Loading

`FlashcardDetailViewModel.init`:

- fires `FlashcardDetailUiIntent.Load`
- calls `FlashcardRepository.fetchById(flashcardId)`
- on error emits `LoadFailed(message)`

## Actions

- `EditFlashcard` → emits `NavigateToEditFlashcard(flashcardId)`
- `DeleteFlashcard` → opens confirmation dialog (`showDeleteConfirmation = true`)
- `ConfirmDeleteFlashcard` → uses `SoftDeleteFlashcardUseCase` and emits `FlashcardDeleted`
- `DismissDeleteFlashcard` → closes dialog

## Effects

`FlashcardDetailUiEffect`:

- `LoadFailed(message)`
- `NavigateToEditFlashcard(flashcardId)`
- `FlashcardDeleted`
- `ShowMessage(text)`

## Persistence

- Read: `FlashcardRepository.fetchById` (local).
- Delete: soft delete via `SoftDeleteFlashcardUseCase`.
- No remote sync involved.

# Current Edit Flashcard

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Edit Flashcard` flow |
| Source of Truth | No |
| Read this when | You need to understand how an existing card's fields are edited |

## Summary

`Edit Flashcard` loads an existing card, lets you edit its basic fields and examples, validates live, and persists the change via `UpdateFlashcardUseCase`. It also exposes an in-screen "Borrar tarjeta" danger row that soft-deletes the card via `SoftDeleteFlashcardUseCase` after a confirmation dialog. Opened from `Card Detail`.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardUiEffect.kt`

## State

`EditFlashcardUiState`:

- `isLoading` (true while the card loads)
- editable fields: `word`, `meaning`, `translation`, `phonetic`, `partOfSpeech`, `examples: List<Example>`
- errors: `wordError`, `meaningError`
- `isSubmitting`
- `isDeleteConfirmationVisible` (toggles the soft-delete confirmation dialog)
- `isValid` (computed): `word` and `meaning` non-empty and no errors

## Load

`EditFlashcardViewModel.init` calls `loadFlashcard()`:

- `FlashcardRepository.fetchById(flashcardId)`
- on success populates fields from `detail.flashcard`
- on error: `ShowMessage` + `isLoading = false`

## Actions

Supported intents:

- `WordChanged(text)` — validates non-blank
- `MeaningChanged(text)` — validates non-blank
- `TranslationChanged(text)`
- `PhoneticChanged(text)`
- `PartOfSpeechChanged(text)`
- `ExampleTextChanged(index, text)`
- `ExampleTranslationChanged(index, translation)`
- `AddExample` — appends an empty `Example`
- `RemoveExample(index)` — bounded by `examples.indices`
- `Submit` — short-circuits if `!isValid || isSubmitting`
- `DeleteFlashcard` — opens the soft-delete confirmation dialog (sets `isDeleteConfirmationVisible = true`)
- `ConfirmDeleteFlashcard` — runs `SoftDeleteFlashcardUseCase` and emits `FlashcardDeleted` on success
- `DismissDeleteFlashcard` — closes the confirmation dialog

## Submit

`handleSubmit()`:

- validates state
- builds `UpdateFlashcardInput(flashcardId, deckId, word, meaning, translation, phonetic, partOfSpeech, examples)`
- calls `UpdateFlashcardUseCase`
- on success: emits `NavigateBack`
- on error: `ShowMessage` + releases `isSubmitting`

## Effects

`EditFlashcardUiEffect`:

- `NavigateBack`
- `FlashcardDeleted`
- `ShowMessage(text)`

## Delete

`handleDelete()` (triggered by `ConfirmDeleteFlashcard`):

- clears `isDeleteConfirmationVisible`
- calls `SoftDeleteFlashcardUseCase(flashcardId)`
- on success: emits `FlashcardDeleted`
- on error: `ShowMessage`

`EditFlashcardViewModel` does **not** emit to `UndoEventHolder`. Undo for card deletion is only wired in `FlashcardDetailViewModel`; deleting from the edit screen navigates away immediately without an undo opportunity.

The screen body renders a `DangerRow` ("Borrar tarjeta", `instrumentBadSoft` background + `instrumentBad` icon/text) below the examples section that dispatches `DeleteFlashcard`. Confirmation uses `HAlertDialog` in dangerous variant.

## Persistence

- Read: `FlashcardRepository.fetchById` (local).
- Write: `UpdateFlashcardUseCase` (local).
- Soft delete: `SoftDeleteFlashcardUseCase` (local).
- No remote sync involved.

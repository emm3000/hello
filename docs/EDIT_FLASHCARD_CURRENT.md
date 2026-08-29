# Current Edit Flashcard

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Edit Flashcard` flow |
| Source of Truth | No |
| Read this when | You need to understand how an existing card's fields are edited |
| Last verified | 2026-08-28 |

## Summary

`Edit Flashcard` loads an existing card, lets you edit six fields (word, translation, first example sentence, its translation, part of speech, phonetic) on the card's own hue, validates the word live, and persists the change via `UpdateFlashcardUseCase`. It also exposes an in-screen "Delete card" text button that soft-deletes the card via `SoftDeleteFlashcardUseCase` after a confirmation dialog. Opened from `Card Detail` (`EditFlashcardRoute(cardId, deckId)`; only `cardId` reaches the destination).

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardUiEffect.kt`

## State

`EditFlashcardUiState`:

- `flashcardId` (seeds the screen hue via `cardHueFor`)
- `isLoading` (true while the card loads)
- editable fields: `word`, `translation`, `exampleText`, `exampleTranslation`, `partOfSpeech`, `phonetic`
- `wordError: Int?` (`@StringRes`)
- `isSubmitting`
- `isDeleteConfirmationVisible` (toggles the soft-delete confirmation dialog)
- `isValid` (computed): `word` non-blank and `wordError == null`

`meaning` and the remaining examples are not editable; the ViewModel keeps the loaded `Flashcard` in a private `loadedCard` and carries them through unchanged on submit.

## Load

`EditFlashcardViewModel.init` calls `loadFlashcard()`:

- `FlashcardRepository.fetchById(flashcardId).flashcard`, stored in `loadedCard`
- on success populates `word`, `translation`, `partOfSpeech`, `phonetic`, and `exampleText` / `exampleTranslation` from `examples.firstOrNull()` (empty if none)
- on error: `isLoading = false` + `ShowMessage(R.string.error_load_card)`

## Actions

Supported intents:

- `WordChanged(word)` — sets `wordError = R.string.validation_word_required` when blank
- `TranslationChanged(translation)`
- `ExampleTextChanged(text)`
- `ExampleTranslationChanged(translation)`
- `PartOfSpeechChanged(partOfSpeech)`
- `PhoneticChanged(phonetic)`
- `CloseClicked` — emits `NavigateBack` without saving
- `Submit` — short-circuits if `!isValid || isSubmitting`
- `DeleteFlashcard` — opens the soft-delete confirmation dialog (sets `isDeleteConfirmationVisible = true`)
- `ConfirmDeleteFlashcard` — runs `SoftDeleteFlashcardUseCase` and emits `FlashcardDeleted` on success
- `DismissDeleteFlashcard` — closes the confirmation dialog

## Submit

`handleSubmit()`:

- validates state
- builds `UpdateFlashcardInput(flashcardId, word, meaning = loadedCard.meaning, translation, phonetic, partOfSpeech, examples = mergedExamples(current))`
- `mergedExamples`: if both example fields are blank the first loaded example is dropped; otherwise the first loaded example is copied with the new `text` / `translation`, or a new `Example(exampleId = "", type = "")` is created when the card had none. Loaded examples after the first are appended untouched.
- calls `UpdateFlashcardUseCase`
- on success: emits `ShowMessage(R.string.card_updated_message)` then `NavigateBack`
- on error: `ShowMessage(R.string.error_save_card)` + releases `isSubmitting`

## Effects

`EditFlashcardUiEffect`:

- `NavigateBack` — `navigator.goBack()`
- `FlashcardDeleted` — `navigator.goBack()`
- `ShowMessage(@StringRes messageRes)` — resolved with `context.getString` and shown as a `Toast`

## Delete

`handleDelete()` (triggered by `ConfirmDeleteFlashcard`):

- clears `isDeleteConfirmationVisible`
- calls `SoftDeleteFlashcardUseCase(flashcardId)`
- on success: emits `FlashcardDeleted`
- on error: `ShowMessage(R.string.error_delete_card)`

`EditFlashcardViewModel` does **not** emit to `UndoEventHolder`. Undo for card deletion is only wired in `FlashcardDetailViewModel`; deleting from the edit screen navigates away immediately without an undo opportunity.

Confirmation uses `HAlertDialog` with `isDangerous = true`, title `R.string.delete_flashcard_title` ("Delete card"), description `R.string.delete_flashcard_description`, confirm `R.string.delete`, cancel `R.string.cancel`.

## Layout

Type sizes, families and color tokens are not repeated here; `EditFlashcardScreen.kt` and `core/theme/` are the only source for them.

The whole screen is a `Surface` colored with `cardHueFor(state.flashcardId)`. A single `Column` with `safeDrawingPadding()` and `MaterialTheme.spacing.screenGutter` horizontal padding holds:

| Block | Content | Notes |
|---|---|---|
| `EditTopBar` | `HIconButton` `Close` (`R.string.edit_flashcard_close_content_description`) dispatching `CloseClicked` · spacer · `HButton` text variant "Save" (`R.string.edit_flashcard_action_save`) dispatching `Submit`. | Save is enabled only when `isValid && !isSubmitting && !isLoading`. Plain `Row`, not `HTopBar`. |
| `LoadingBody` | Centered `HLoadingSpinner`. | Shown while `isLoading`. |
| `FieldList` | Uppercase caption "Edit card" (`R.string.edit_flashcard_title`), then six `HInput` fields in `HFieldVariant.Underline`: word (`word_label`, error from `wordError`), translation (`translation_label`), example sentence (`example_sentence_label`, multiline), example translation (`example_translation_label`, multiline), part of speech (`part_of_speech_label`), phonetics (`phonetics_label`). | Placeholders come from `edit_flashcard_*_placeholder`. Scrollable. |
| Delete | `HButton` text variant with `danger = true`, "Delete card" (`R.string.edit_flashcard_delete_action`), dispatching `DeleteFlashcard`. | Sits below the fields inside the scrollable column. |

## Persistence

- Read: `FlashcardRepository.fetchById` (local).
- Write: `UpdateFlashcardUseCase` (local).
- Soft delete: `SoftDeleteFlashcardUseCase` (local).
- No remote sync involved.

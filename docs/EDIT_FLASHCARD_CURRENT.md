# Current Edit Flashcard

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Edit Flashcard` flow |
| Source of Truth | No |
| Read this when | You need to understand how an existing card's fields are edited |

## Summary

`Edit Flashcard` loads an existing card, lets you edit its basic fields and examples, validates live, and persists the change via `UpdateFlashcardUseCase`. Opened from `Card Detail`.

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
- `ShowMessage(text)`

## Persistence

- Read: `FlashcardRepository.fetchById` (local).
- Write: `UpdateFlashcardUseCase` (local).
- No remote sync involved.

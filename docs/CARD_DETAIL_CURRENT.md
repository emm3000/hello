# Current Card Detail

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Card Detail` flow |
| Source of Truth | No |
| Read this when | You need to understand how an existing card is shown and deleted |

## Summary

`Card Detail` shows a saved flashcard as a dictionary entry and lets you edit or delete it (soft delete). It opens from Biblioteca. Single vertical scroll, no tabs.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/card/CardDetailRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/core/ui/HDictSense.kt` (component scaffolded for this screen)

## State

`FlashcardDetailUiState` holds:

- `flashcard: FlashcardDetail` (empty default with `SystemClock`)
- `isLoading: Boolean = true`
- `isDeleteConfirmationVisible`

## Loading

`FlashcardDetailViewModel.init`:

- fires `FlashcardDetailUiIntent.Load`
- calls `FlashcardRepository.fetchById(flashcardId)`
- on success sets `flashcard` and flips `isLoading = false`
- on error emits `LoadFailed(message)`

`isLoading` starts at `true`, so the screen never flashes the empty-flashcard default: while it is `true`, `FlashcardDetailScreen` keeps the top bar and renders `LoadingBody()` instead of the entry body.

## Actions

- `EditFlashcard` (edit icon in top bar, no longer in dropdown) → emits `NavigateToEditFlashcard(flashcardId)`
- `DeleteFlashcard` (inside "more" dropdown) → opens confirmation dialog (`isDeleteConfirmationVisible = true`)
- `ConfirmDeleteFlashcard` → uses `SoftDeleteFlashcardUseCase`, emits `FlashcardDeleted` effect, and also emits `UndoEvent.CardDeleted` to `UndoEventHolder` so `LibraryViewModel` can show an undo snackbar
- `DismissDeleteFlashcard` → closes dialog

## Effects

`FlashcardDetailUiEffect`:

- `LoadFailed(message)`
- `NavigateToEditFlashcard(flashcardId)`
- `FlashcardDeleted`
- `ShowMessage(text)`

## Layout

Type sizes, families and color tokens are not repeated here; `FlashcardDetailScreen.kt` and `core/theme/` are the only source for them.

Single `Column` inside a `verticalScroll`. All blocks share `horizontal padding = 20.dp`. Sections are separated by `HSeparator` with 24.dp vertical margin.

| Block | Content | Notes |
|---|---|---|
| `HTopBar` | back · edit · more (delete in dropdown) | Transparent, status bar padding included by the parent `Column`. |
| Hero | `HSectionLabel("Tarjeta")`, the word, a meta row (phonetic, `partOfSpeech`, `levelBand`), then `translation`. | Meta row and translation render only if their fields are non-blank. |
| Dict senses | Dynamic numbered list using `HDictSense`. Drawn only when at least one field below is non-empty. | Order: `meaning` (Default), `usagePattern` (Default), `whyUseful` (Default), `commonMistake` (Warn), `confusableWith` joined by `, ` (Warn), `noteSummary` (Default). Indices restart at 1 and only count non-empty entries. |
| Examples | `HSectionLabel("Ejemplos · N")` + each example as EN over a muted ES translation. | Skipped if `examples` is empty. |
| Extras | `HSectionLabel("Más data")` + per-list sublabels (mono uppercase) for `collocations` and `irregularForms` (rendered as `HChip` flow rows), a bullet list for `warnings`, then mono-label rows for `register`/`learningDomain`/`lemma`. | Block hidden entirely if all sources are empty. |
| Context | `HSectionLabel("Contexto")` + the cloze over a muted source context. | Skipped if both fields are blank. |
| Footer | Mono row `"N tarjetas de estudio · K con advertencia · próxima en Xd"`. | Pieces appear conditionally; failed-check count is rendered in accent when `> 0`, otherwise the whole row is instrumentFaint. |

## Persistence

- Read: `FlashcardRepository.fetchById` (local).
- Delete: soft delete via `SoftDeleteFlashcardUseCase`.
- No remote sync involved.

## Strings touched in Phase 2.4

Added (`app/src/main/res/values/strings.xml`):

- `card_detail_header_label`
- `card_detail_examples_section_label` (with `%1$d`)
- `card_detail_extras_section_label`
- `card_detail_context_section_label`
- `card_detail_footer_study_cards_one` / `_other`
- `card_detail_footer_warnings_one` / `_other`
- `card_detail_footer_next_today` / `_overdue` / `_days_one` / `_days_other`

Modified value (key unchanged):

- `confusable_with_label` → "No confundir con" (was "Se confunde con") to match the Warn-tone `HDictSense` block.

The legacy `notes_label`, `cloze_sentence_label`, `source_context_label`, `learning_metadata_label`, `register_label`, `level_label`, `domain_label`, `lemma_label`, `generated_cards_label`, `quality_checks_label`, `prompt_label`, `expected_answer_label`, `hint_label`, `show_translation`, `hide_translation` keys remain in `strings.xml` — some are still referenced from `NewCardPreviewComponents.kt` / `NewCardPreviewCards.kt`. The Phase 5 cleanup (`fe221b3`) did not remove them because they are still in use.

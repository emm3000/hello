# Current Card Detail

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Card Detail` flow |
| Source of Truth | No |
| Read this when | You need to understand how an existing card is shown and deleted |
| Last verified | 2026-08-28 |

## Summary

`Card Detail` shows a saved flashcard at rest on its own hue and lets you edit or delete it (soft delete). It opens from `Library` (`LibraryRoute` navigates to `CardDetailRoute(cardId, deckId)`). Single vertical scroll, no tabs, no sections.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/card/CardDetailRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/core/ui/TextEmphasis.kt` (`underlineFirstMatch`, used to underline the word inside the example)

## State

`FlashcardDetailUiState` holds:

- `flashcard: Flashcard` (default `Flashcard.empty(SystemClock)`)
- `isLoading: Boolean = true`
- `isDeleteConfirmationVisible: Boolean = false`

## Loading

`FlashcardDetailViewModel.init`:

- fires `FlashcardDetailUiIntent.Load`
- calls `FlashcardRepository.fetchById(flashcardId)`
- on success sets `flashcard = detail.flashcard` and flips `isLoading = false`
- on error emits `LoadFailed("Couldn't load the card")` (hard-coded literal, not a string resource); `CardDetailDestination` shows it as a `Toast` and navigates back

`isLoading` starts at `true`, so the screen never flashes the empty-flashcard default: while it is `true`, `FlashcardDetailScreen` keeps the top bar and renders `LoadingBody()` (a centered `HLoadingSpinner`) instead of the card body.

## Actions

- `Load` → `loadFlashcard()`
- `BackClicked` (back icon in top bar) → emits `NavigateBack`
- `EditFlashcard` ("Edit" text button in top bar) → emits `NavigateToEditFlashcard(flashcardId)`
- `DeleteFlashcard` ("Delete" destructive item inside the "more" dropdown) → opens confirmation dialog (`isDeleteConfirmationVisible = true`)
- `ConfirmDeleteFlashcard` → closes the dialog, runs `SoftDeleteFlashcardUseCase`, emits `UndoEvent.CardDeleted(flashcardId, deletedAt)` to `UndoEventHolder` so `LibraryViewModel` can show an undo snackbar, then emits `FlashcardDeleted`; on error emits `ShowMessage("Couldn't delete the card")` (hard-coded literal)
- `DismissDeleteFlashcard` → closes dialog

## Effects

`FlashcardDetailUiEffect`:

- `LoadFailed(message)` — toast + `navigator.goBack()`
- `NavigateBack` — `navigator.goBack()`
- `NavigateToEditFlashcard(cardId)` — `navigator.navigateTo(EditFlashcardRoute(cardId, deckId))`
- `FlashcardDeleted` — `navigator.goBack()`
- `ShowMessage(message)` — toast

## Layout

Type sizes, families and color tokens are not repeated here; `FlashcardDetailScreen.kt` and `core/theme/` are the only source for them.

The whole screen is a `Surface` colored with `cardHueFor(flashcard.id.value)`. Inside, a single `Column` with `safeDrawingPadding()` and `MaterialTheme.spacing.screenGutter` horizontal padding holds the top bar and the body. The body is a `Column` inside a `verticalScroll`, blocks spaced by 20.dp. No `HSeparator`, `HSectionLabel`, `HTopBar` or `HCard` is used.

| Block | Content | Notes |
|---|---|---|
| `DetailTopBar` | `HIconButton` back (`ArrowBack`) · spacer · `HButton` text variant "Edit" (`R.string.edit`) · `HIconButton` `MoreVert` (`R.string.more_options`) opening an `HDropdownMenu` with one destructive `HMenuItem` "Delete" (`R.string.delete`). | Plain `Row`, min height 44.dp; not `HTopBar`. |
| `WordBlock` | The word, then `phonetic`. | `phonetic` renders only if non-blank. |
| Translation | `translation` as a large display line. | Rendered only if non-blank. |
| `ExampleBlock` | First example (`examples.firstOrNull()`): text with the word underlined via `underlineFirstMatch`, then its translation. | Skipped if there is no example or its `text` is blank; translation only if non-blank. |
| `ReferenceLine` | `partOfSpeech` and `meaning` joined by ` · `. | Blank parts are dropped; line skipped if nothing remains. |
| `StatusLine` | `enrichmentStatus`: `PENDING` → "Preparing…" (`R.string.library_status_pending`), `FAILED` → "Failed" (`R.string.library_status_failed`, destructive ink). | `ENRICHED` renders nothing. |

The delete confirmation is an `HAlertDialog` with `isDangerous = true`, title `R.string.delete_flashcard_title` ("Delete card"), description `R.string.delete_flashcard_description`, confirm `R.string.delete`, cancel `R.string.cancel`.

## Persistence

- Read: `FlashcardRepository.fetchById` (local).
- Delete: soft delete via `SoftDeleteFlashcardUseCase`.
- No remote sync involved.

## Strings

Keys referenced by `FlashcardDetailScreen.kt` (`app/src/main/res/values/strings.xml`, English copy):

- `edit`, `delete`, `cancel`, `more_options`
- `delete_flashcard_title`, `delete_flashcard_description`
- `library_status_pending`, `library_status_failed`

The load and delete error messages emitted by `FlashcardDetailViewModel` are hard-coded English literals, not resources.

The former `card_detail_*` keys and `confusable_with_label` no longer exist in `strings.xml`; the dictionary-entry layout (`HDictSense`, senses, examples list, extras, context, footer) was removed with the redesign.

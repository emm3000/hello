# Current Capture

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Capturar` flow (bare-word capture + background enrichment) |
| Source of Truth | No |
| Read this when | You need to understand how a word enters the app today and what happens to it after Save |
| Last verified | 2026-08-28 |

## Summary

`Capturar` is the only card-creation path. The user types or dictates one
English word, taps Save, and the card is written to `HelloDb` immediately with
`EnrichmentStatus.PENDING` and empty meaning/phonetic. A WorkManager job then
fills the card in from Firebase AI. The screen never shows a preview, never
lets the user edit the generated note, and has no deck picker: the target deck
is the default deck (falling back to the first deck). Editing the result is
`EDIT_FLASHCARD_CURRENT.md`.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/capture/CaptureRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/capture/CaptureScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/capture/CaptureViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/capture/CaptureUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/capture/CaptureUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/capture/CaptureUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/enrichment/FlashcardEnrichmentScheduler.kt`
- `app/src/main/kotlin/com/emm/hello/enrichment/FlashcardEnrichmentWorker.kt`
- `domain/src/main/kotlin/com/emm/domain/authoring/CaptureFlashcardUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/authoring/EnrichCapturedFlashcardUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/authoring/RetryFailedEnrichmentsUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/authoring/MarkEnrichmentFailedUseCase.kt`

Entry points: `NewRoot` registers `CaptureRoute`; `Hoy` (FAB and the
nothing-due CTA), `Biblioteca` (`OpenCapture`) and `Study`
(`NavigateToCapture`) all navigate to it.

## State

`CaptureUiState` holds six fields:

- `word: String` — the text field content
- `targetDeck: Deck?` — resolved at init from `GetDecksUseCase` +
  `DefaultDeckSelectionRepository.getDefaultDeckId()`; the default deck if it
  exists, else the first deck, else `null`
- `isSaving: Boolean` — true while `CaptureFlashcardUseCase` runs
- `pending: Int` / `failed: Int` — from
  `FlashcardEnrichmentRepository.observeBacklog()` (`EnrichmentBacklog`),
  refreshed on every DB change
- `recentCaptures: List<RecentCapture>` — the words saved in this ViewModel
  instance, newest first; each has `flashcardId`, `word` and
  `status: EnrichmentStatus`. Statuses are refreshed from
  `LibraryRepository.observeLibrary()`, so a card flips from `PENDING` to
  `ENRICHED` / `FAILED` while the screen is open. The list is not persisted
  and starts empty on every visit.

Two values are computed, not stored:

- `canSubmit` — `word.isNotBlank() && targetDeck != null && !isSaving`
- `hasBacklog` — `pending > 0 || failed > 0` (declared, not read by the screen)

`RecentCapture` lives in `CaptureUiState.kt`.

## Intents

`CaptureUiIntent`:

- `WordChanged(word)` — replaces `word`. Also fired by the speech-to-text
  result, which overwrites the field rather than appending.
- `Submit` — no-op unless `canSubmit`. Sets `isSaving`, calls
  `CaptureFlashcardUseCase(deckId, word)` (the use case's optional
  `translation` parameter is not passed; it defaults to `""`). On success:
  clears `word`, prepends a `RecentCapture` with `PENDING`, then emits
  `EnqueueEnrichment([id])` followed by `ShowMessage(capture_saved_message)`.
  On `DomainValidationException`: `DuplicateWordInDeck` maps to
  `capture_error_duplicate`, `EmptyUserText` to `capture_error_empty`, anything
  else to `capture_error_generic`; nothing is enqueued. Any other throwable
  logs and shows `capture_error_generic`.
- `RetryFailed` — calls `RetryFailedEnrichmentsUseCase`, which flips every
  `FAILED` card back to `PENDING` and returns their ids. If the list is
  non-empty, emits `EnqueueEnrichment(ids)`; if empty, emits nothing. On error,
  `ShowMessage(capture_error_retry)`.

## Effects

`CaptureUiEffect`, collected in `CaptureDestination`:

- `ShowMessage(@StringRes messageRes)` — shown as a short `Toast`.
- `EnqueueEnrichment(flashcardIds: List<String>)` — each id is passed to
  `FlashcardEnrichmentScheduler.enqueue(context, id)`.

### Enrichment pipeline

`FlashcardEnrichmentScheduler.enqueue` schedules one unique
`OneTimeWorkRequest` per card (`flashcard_enrichment_<id>`,
`ExistingWorkPolicy.REPLACE`) with `NetworkType.CONNECTED` and exponential
backoff starting at 5 minutes.

`FlashcardEnrichmentWorker` resolves `EnrichCapturedFlashcardUseCase` from
Koin, which reads the stored word, calls
`FlashcardGenerationRepository.generateLearningNote` with
`FlashcardInputType.Word`, validates the note, writes it back through
`repository.update` + `upsertExamples`, and sets the status to `ENRICHED`. Any
failure returns `Result.retry()` until `MAX_ATTEMPTS = 3`; on the last attempt
`MarkEnrichmentFailedUseCase` sets `FAILED` and the worker gives up. A card in
`FAILED` is what `RetryFailed` picks up.

## Screen

Full-screen `cardMint` surface, no scaffold. Top to bottom:

- **Header** — `capture_title` ("Add a word") uppercased in `schibsted` 13 sp
  with wide tracking, and a text `HButton` `capture_done` ("Done") that calls
  `navigator::goBack`.
- **Input row** — `HInput` (`HFieldVariant.Underline`, placeholder
  `capture_placeholder`, disabled while `isSaving`) plus a 44 dp `HIconButton`
  mic. The mic icon is `Mic` while listening and `MicNone` otherwise.
- **Recent list** — rendered only when `recentCaptures` is non-empty: the
  `capture_recent_label` ("Your last:") caption, then one row per capture with
  the word in semibold and the status label at the trailing edge
  (`capture_status_preparing` / `capture_status_ready` /
  `capture_status_failed`).
- **Retry** — a text `HButton` `capture_retry` rendered only when
  `failed > 0`. `pending` is not surfaced anywhere on the screen.
- **Save** — full-width primary `HButton` `capture_save`, `enabled = canSubmit`,
  `isLoading = isSaving`.

The input, recent list and retry are vertically centered in the space between
header and Save.

Dictation uses `rememberSpeechToTextManager` with `Locale.US`. Tapping the mic
stops if listening, starts if `RECORD_AUDIO` is granted, otherwise launches the
permission request; a denial shows `mic_permission_denied` in a `SnackbarHost`
at the bottom. STT errors are shown in the same snackbar and cleared.

All copy is English and comes from `capture_*` strings in
`app/src/main/res/values/strings.xml`.

## Not in scope / Related docs

- No deck picker, no default-deck checkbox, no hints, no difficulty, no
  preview, no quota warning, no in-screen editing of the generated note.
- Zero decks: `targetDeck` stays `null`, Save is disabled and no message
  explains why. Decks are managed in Settings → Mazos (`DECK_CURRENT.md`).
- Reading the enriched card: `CARD_DETAIL_CURRENT.md`.
- Editing the card after enrichment: `EDIT_FLASHCARD_CURRENT.md`.
- Where captures are listed and searched: `LIBRARY_CURRENT.md`.
- Home CTA that opens this screen: `TODAY_CURRENT.md`.

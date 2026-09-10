# Current Capture

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Capturar` flow (bare-word capture + background enrichment, plus the manual write-it-myself mode) |
| Source of Truth | No |
| Read this when | You need to understand how a word enters the app today and what happens to it after Save |
| Last verified | 2026-09-09 |

## Summary

`Capturar` is the only card-creation path. The user types or dictates one
English word, taps Save, and the card is written to `HelloDb` immediately with
`EnrichmentStatus.PENDING` and empty meaning/phonetic. A WorkManager job then
fills the card in from Firebase AI. A **manual mode** sits behind a toggle under
the word field: the user writes the Spanish translation (required) and
optionally an English meaning, and Save writes the card straight to `HelloDb`
as `ENRICHED` — no AI, no network, no enrichment job — so it works offline and
is studiable immediately. The screen never shows a preview, never lets the user
edit the generated note, and has no deck picker: the target deck is the default
deck (falling back to the first deck). Editing the result is
`EDIT_FLASHCARD_CURRENT.md`.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/capture/CaptureRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/capture/CaptureScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/capture/CaptureViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/capture/CaptureUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/capture/CaptureUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/capture/CaptureUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/capture/GenerationRefusalCodeMessage.kt`
- `domain/src/main/kotlin/com/emm/domain/generation/EnrichmentFailure.kt`
- `domain/src/main/kotlin/com/emm/domain/generation/GenerationRefusalCode.kt`
- `domain/src/main/kotlin/com/emm/domain/connectivity/ConnectivityRepository.kt`
- `data/src/main/kotlin/com/emm/data/connectivity/AndroidConnectivityRepository.kt`
- `app/src/main/kotlin/com/emm/hello/enrichment/FlashcardEnrichmentScheduler.kt`
- `app/src/main/kotlin/com/emm/hello/enrichment/FlashcardEnrichmentWorker.kt`
- `domain/src/main/kotlin/com/emm/domain/authoring/CaptureFlashcardUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/authoring/CreateManualFlashcardUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/authoring/EnrichCapturedFlashcardUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/authoring/RetryFailedEnrichmentsUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/authoring/MarkEnrichmentFailedUseCase.kt`

Entry points: `NewRoot` registers `CaptureRoute`; `Hoy` (the "Add a word"
button, present in both its session-ready and nothing-due layouts),
`Biblioteca` (`OpenCapture`) and `Study` (`NavigateToCapture`) all navigate
to it.

## State

`CaptureUiState` holds ten fields:

- `word: String` — the text field content
- `targetDeck: Deck?` — resolved at init from `GetDecksUseCase` +
  `DefaultDeckSelectionRepository.getDefaultDeckId()`; the default deck if it
  exists, else the first deck, else `null`
- `isSaving: Boolean` — true while `CaptureFlashcardUseCase` runs
- `pending: Int` / `failed: Int` — from
  `FlashcardEnrichmentRepository.observeBacklog()` (`EnrichmentBacklog`),
  refreshed on every DB change
- `recentCaptures: List<RecentCapture>` — the words saved in this ViewModel
  instance, newest first; each has `flashcardId`, `word`, `status:
  EnrichmentStatus` and a nullable `failure: EnrichmentFailure`
  (`code: GenerationRefusalCode?`, `reason: String?`, both from
  `com.emm.domain.generation`). Statuses and `failure` are refreshed from
  `LibraryRepository.observeLibrary()`, so a card flips from `PENDING` to
  `ENRICHED` / `FAILED` while the screen is open. The list is not persisted
  and starts empty on every visit.
- `isOnline: Boolean` — mirrors `ConnectivityRepository.observeOnline()`,
  collected in `init`; defaults `true` and only changes the `PENDING` status
  label, never `canSubmit` or Save.
- `isManual: Boolean` — defaults `false`; `true` while the user is writing the
  card by hand instead of letting the AI fill it in. It survives a manual save.
- `translation: String` — the Spanish translation, required in manual mode
- `meaning: String` — the optional English meaning, manual mode only

Two values are computed, not stored:

- `canSubmit` — `word.isNotBlank() && targetDeck != null && !isSaving &&
  (!isManual || translation.isNotBlank())`
- `hasBacklog` — `pending > 0 || failed > 0` (declared, not read by the screen)

`RecentCapture` lives in `CaptureUiState.kt`.

## Intents

`CaptureUiIntent`:

- `WordChanged(word)` — replaces `word`. Also fired by the speech-to-text
  result, which overwrites the field rather than appending.
- `ManualModeToggled` — flips `isManual`. Leaving manual mode clears
  `translation` and `meaning`; `word` is left untouched.
- `TranslationChanged(translation)` — replaces `translation`.
- `MeaningChanged(meaning)` — replaces `meaning`.
- `Submit` — no-op unless `canSubmit`. Sets `isSaving`, then branches on
  `isManual`. With `isManual = false` it calls
  `CaptureFlashcardUseCase(deckId, word)` (the use case's optional
  `translation` parameter is not passed; it defaults to `""`). On success:
  clears `word`, prepends a `RecentCapture` with `PENDING`, then emits
  `EnqueueEnrichment([id])` followed by `ShowMessage(capture_saved_message)`.
  With `isManual = true` it calls
  `CreateManualFlashcardUseCase(deckId, word, translation, meaning)`, which
  writes the card as `ENRICHED`. On success: clears `word`, `translation` and
  `meaning`, keeps `isManual = true`, prepends a `RecentCapture` with
  `ENRICHED` and emits only `ShowMessage(capture_saved_ready_message)` — no
  `EnqueueEnrichment`, so no worker and no network are involved.
  On `DomainValidationException`: `DuplicateWordInDeck` maps to
  `capture_error_duplicate`, `EmptyTranslation` to
  `capture_error_translation_required`, `EmptyUserText` to
  `capture_error_empty`, anything else to `capture_error_generic`; nothing is
  enqueued. Any other throwable logs and shows `capture_error_generic`.
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
`repository.update`, records the prompt version, then `upsertExamples`, and
sets the status to `ENRICHED`. On a validation failure the use case
regenerates once, feeding the rejected issue codes back into the prompt,
before giving up. `EnrichmentRetryPolicy.shouldRetry` decides per error: a
`DomainValidationException`, `AmbiguousGenerationInputException`,
`AppCheckRejectedException` or `GenerationCreditsExhaustedException` is not
retried — the worker marks the card `FAILED` on the first attempt through
`MarkEnrichmentFailedUseCase`; a `SessionExpiredException` or anything else
(timeout, 5xx, an unrecognized error) returns `Result.retry()` until
`MAX_ATTEMPTS = 3`, then also marks it `FAILED`. A card in `FAILED`
is what `RetryFailed` picks up. A `FAILED` card can also be completed by hand,
without retrying generation, by filling in its meaning from Edit Flashcard —
see `EDIT_FLASHCARD_CURRENT.md`.

## Screen

Full-screen `cardMint` surface, no scaffold. Top to bottom:

- **Header** — `capture_title` ("Add a word") uppercased in `schibsted` 13 sp
  with wide tracking, and a text `HButton` `capture_done` ("Done") that calls
  `navigator::goBack`.
- **Input row** — `HInput` (`HFieldVariant.Underline`, placeholder
  `capture_placeholder`, disabled while `isSaving`) plus a 44 dp `HIconButton`
  mic. The mic icon is `Mic` while listening and `MicNone` otherwise.
- **Mode toggle** — a text `HButton` directly under the input row, reading
  `capture_manual_toggle_write` ("Write it myself") in AI mode and
  `capture_manual_toggle_ai` ("Let AI write it") in manual mode.
- **Manual fields** — rendered only while `isManual`: two underline `HInput`s,
  the required `capture_manual_translation_label` ("Spanish translation",
  single line, `ImeAction.Next`) and the optional
  `capture_manual_meaning_label` ("Meaning in English (optional)", 2 to 4
  lines). Both are disabled while `isSaving`.
- **Recent list** — rendered only when `recentCaptures` is non-empty: the
  `capture_recent_label` ("Your last:") caption, then one row per capture with
  the word in semibold and the status label at the trailing edge. A `PENDING`
  card reads `capture_status_preparing` while `state.isOnline`, or
  `capture_status_waiting_for_connection` while offline; `ENRICHED` always
  reads `capture_status_ready` and `FAILED` always reads
  `capture_status_failed`. A `FAILED` row renders a second line through the
  private `CaptureFailureMessage` composable: a known `failure.code` shows
  the localized string from `GenerationRefusalCode.messageRes()`
  (`capture_failure_empty_input`, `capture_failure_unintelligible`,
  `capture_failure_contradictory`, `capture_failure_unmappable`,
  `capture_failure_credits_exhausted`, in `values` and `values-es`);
  otherwise the raw `failure.reason` is shown; otherwise nothing is shown.
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
- Manual mode has no phonetic, no examples and no enrichment fallback: a card
  written by hand is never sent to the AI afterwards.
- Zero decks: `targetDeck` stays `null`, Save is disabled and no message
  explains why. Decks are managed in Settings → Mazos (`DECK_CURRENT.md`).
- Reading the enriched card: `CARD_DETAIL_CURRENT.md`.
- Editing the card after enrichment: `EDIT_FLASHCARD_CURRENT.md`.
- Where captures are listed and searched: `LIBRARY_CURRENT.md`.
- Home CTA that opens this screen: `TODAY_CURRENT.md`.

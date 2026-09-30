# Current Capture

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Capturar` flow (bare-word capture + background enrichment, plus the manual write-it-myself mode) |
| Source of Truth | No |
| Read this when | You need to understand how a word enters the app today and what happens to it after Save |
| Last verified | 2026-09-29 |

## Summary

`Capturar` is the only card-creation path. The user types or dictates one
English word, taps Save, and the card is written to `HelloDb` immediately with
`EnrichmentStatus.PENDING` and empty meaning/phonetic. A WorkManager job then
fills the card in from Firebase AI. A **manual mode** sits behind the mode row
under the word field, which names the active mode and offers the other one: the
user writes the Spanish translation (required) and
optionally an English meaning, and Save writes the card straight to `HelloDb`
as `ENRICHED` — no AI, no network, no enrichment job — so it works offline and
is studiable immediately. The screen never shows a preview, never lets the user
edit the generated note. The target deck comes from `ResolveCaptureDeckUseCase`;
a "Saving to" picker appears only when the user has more than one deck of their
own, so decks installed from the Store never surface it. Editing the result is
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

`CaptureUiState` holds fourteen fields:

- `word: String` — the text field content
- `targetDeck: Deck?` and `decks: List<Deck>` — refreshed on every
  `GetDecksUseCase` emission through `ResolveCaptureDeckUseCase`. `decks` are
  the capture candidates: the user's own decks, or every live deck when only
  Store installs (`curated-<id>`) exist. `targetDeck` is the stored default
  deck if it is a candidate, else the oldest candidate, else `null`
- `isDeckPickerOpen: Boolean` — the "Saving to" dropdown, shown only when
  `decks` has more than one entry
- `isSaving: Boolean` — true while `CaptureFlashcardUseCase` or
  `CreateManualFlashcardUseCase` runs; it blocks a second `Submit` but does
  not disable the text fields
- `pending: Int` / `failed: Int` — from
  `FlashcardEnrichmentRepository.observeBacklog()` (`EnrichmentBacklog`),
  refreshed on every DB change
- `recentCaptures: List<RecentCapture>` — the words saved in this ViewModel
  instance, newest first; each has `flashcardId`, `deckId`, `word`, `status:
  EnrichmentStatus` and a nullable `failure: EnrichmentFailure`
  (`code: GenerationRefusalCode?`, `reason: String?`, both from
  `com.emm.domain.generation`). `deckId` starts as the target deck at save
  time. `deckId`, statuses and `failure` are refreshed from
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
- `wordErrorRes: Int?` — `@StringRes` shown as the word field's inline error;
  set to `capture_error_duplicate` on `DuplicateWordInDeck`, cleared by the
  next `WordChanged`
- `translationErrorRes: Int?` — `@StringRes` shown as the translation field's
  inline error; set to `capture_error_translation_required`, cleared by the
  next `TranslationChanged` or by switching to AI mode

Two values are computed, not stored:

- `canSubmit` — `word.isNotBlank() && targetDeck != null && !isSaving &&
  (!isManual || translation.isNotBlank())`
- `hasBacklog` — `pending > 0 || failed > 0` (declared, not read by the screen)

`RecentCapture` lives in `CaptureUiState.kt`.

## Intents

`CaptureUiIntent`:

- `WordChanged(word)` — replaces `word` and clears `wordErrorRes`. Also fired by the speech-to-text
  result, which overwrites the field rather than appending.
- `ManualModeSelected(isManual)` — sets `isManual` to the requested value.
  Selecting AI mode clears `translation`, `meaning` and `translationErrorRes`;
  `word` is left
  untouched. Selecting the mode that is already active changes nothing.
- `TranslationChanged(translation)` — replaces `translation` and clears
  `translationErrorRes`.
- `MeaningChanged(meaning)` — replaces `meaning`.
- `Submit` — fired by Save and by the keyboard Send action. Ignored silently
  while `isSaving`, with a blank `word` or with no `targetDeck`, so a double
  tap never saves twice. In manual mode a blank `translation` sets
  `translationErrorRes` and saves nothing (reachable through the Send key;
  Save stays disabled by `canSubmit`). Otherwise sets `isSaving` and branches
  on `isManual`. With `isManual = false` it calls
  `CaptureFlashcardUseCase(deckId, word)` (the use case's optional
  `translation` parameter is not passed; it defaults to `""`). On success:
  clears `word`, prepends a `RecentCapture` with `PENDING` and emits
  `EnqueueEnrichment([id])`. With `isManual = true` it calls
  `CreateManualFlashcardUseCase(deckId, word, translation, meaning)`, which
  writes the card as `ENRICHED`. On success: clears `word`, `translation` and
  `meaning`, keeps `isManual = true` and prepends a `RecentCapture` with
  `ENRICHED`; it emits no effect, so no worker and no network are involved.
  Neither success path shows a message: the new recent row is the
  confirmation. On `DomainValidationException`: `DuplicateWordInDeck` sets
  `wordErrorRes` and keeps `word`, `EmptyTranslation` sets
  `translationErrorRes`, `EmptyUserText` is ignored, anything else emits
  `ShowMessage(capture_error_generic)`; nothing is enqueued. Any other
  throwable logs and emits `ShowMessage(capture_error_generic)`.
- `RetryFailed` — calls `RetryFailedEnrichmentsUseCase`, which flips every
  `FAILED` card back to `PENDING` and returns their ids. If the list is
  non-empty, emits `EnqueueEnrichment(ids)`; if empty, emits nothing. On error,
  `ShowMessage(capture_error_retry)`.
- `RecentCaptureClicked(flashcardId)` — emits `OpenCard(cardId, deckId)` for
  that recent capture; ignored if the id is not in `recentCaptures`.
- `DeckPickerOpened` / `DeckPickerDismissed` — toggle `isDeckPickerOpen`.
- `DeckSelected(deckId)` — ignored unless the deck is among `decks`; otherwise
  stores it with `DefaultDeckSelectionRepository.setDefaultDeckId`, makes it
  `targetDeck` and closes the picker.

## Effects

`CaptureUiEffect`, collected in `CaptureDestination`:

- `ShowMessage(@StringRes messageRes)` — shown in the screen's `SnackbarHost`
  (the `SnackbarHostState` is created in `CaptureDestination` and passed to
  `CaptureScreen`). Only transient errors use it; Capture shows no `Toast`.
- `EnqueueEnrichment(flashcardIds: List<String>)` — each id is passed to
  `FlashcardEnrichmentScheduler.enqueue(context, id)`.
- `OpenCard(cardId, deckId)` — `navigator.navigateTo(CardDetailRoute(cardId,
  deckId))`. The Capture ViewModel survives the round trip, so the recent list
  is still there on return.

### Enrichment pipeline

`FlashcardEnrichmentScheduler.enqueue` schedules one unique
`OneTimeWorkRequest` per card (`flashcard_enrichment_<id>`,
`ExistingWorkPolicy.KEEP`) with `NetworkType.CONNECTED` and exponential
backoff starting at 5 minutes. `KEEP` means a requeue of a card that already
has enrichment work pending or running (for example `AppStartupCoordinator`
requeuing every `PENDING` card on launch) never cancels an in-flight worker
whose HTTP request the backend may have already charged.

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
  `capture_placeholder`, inline error from `wordErrorRes`) plus a 44 dp
  `HIconButton` mic. Its IME action is `Send` (submits) in AI mode and `Next`
  (moves focus to the translation field) in manual mode. The mic icon is `Mic` while listening and `MicNone` otherwise.
- **Mode row** — always rendered, in both modes and in the same position, with
  two fixed roles. At the leading edge, a `bodySmall` `inkSoft` caption
  naming the active mode: `capture_mode_ai_label` ("AI completes it") while
  `isManual` is false, `capture_mode_manual_label` ("You write it") while it is
  true. At the trailing edge, a text-variant `HButton` whose label is
  underlined semibold `labelLarge`, and which switches to the other mode:
  `capture_mode_manual_action` ("Write it myself") while `isManual` is false,
  `capture_mode_ai_action` ("Use AI") while it is true. Only the copy inside
  each role changes; the roles never swap. The action is disabled while
  `isSaving`.
- **Manual fields** — rendered only while `isManual`: two single-line
  underline `HInput`s, the required `capture_manual_translation_label`
  ("Spanish translation", `ImeAction.Next` to the meaning field, inline error
  from `translationErrorRes`; the field takes focus when that error appears)
  and the optional `capture_manual_meaning_label` ("Meaning in English
  (optional)", `ImeAction.Send`, submits). No text field is disabled while
  `isSaving`.
- **Recent list** — rendered only when `recentCaptures` is non-empty: the
  `capture_recent_label` ("Your last:") caption, then one row per capture:
  the word in `titleMedium`, taking the remaining width and wrapping, then the
  `bodySmall` status label and a `KeyboardArrowRight` chevron, which never
  shrink. Every row, whatever its status, is clickable (at least the minimum
  interactive size) and fires `RecentCaptureClicked`, which opens Card Detail.
  Each row's content description is `capture_recent_row_description` (word
  and status). The newest row carries a polite live region, so TalkBack
  announces a save and its later status change, and when it appears after a
  save its background flashes the `hairline` token and fades out over 1.5 s. A `PENDING`
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

After a successful save in either mode, focus returns to the word field and
the soft keyboard stays up. This is driven by the newest `RecentCapture` id,
not by an effect; the id already present when the screen is composed (first
visit or return from Card Detail) is remembered, so neither steals focus.

The input, mode row, recent list and retry are top-anchored in the space
between header and Save, so entering manual mode grows the block downward
instead of shifting what is already on screen.

Dictation uses `rememberSpeechToTextManager` with `Locale.US`. Tapping the mic
stops if listening, starts if `RECORD_AUDIO` is granted, otherwise launches the
permission request; a denial shows `mic_permission_denied` in a `SnackbarHost`
at the bottom. STT errors, save errors other than the inline ones and retry
errors are shown in the same snackbar.

All copy is English and comes from `capture_*` strings in
`app/src/main/res/values/strings.xml`.

## Not in scope / Related docs

- No default-deck checkbox, no hints, no difficulty, no
  preview, no quota warning, no in-screen editing of the generated note.
- Manual mode has no phonetic, no examples and no enrichment fallback: a card
  written by hand is never sent to the AI afterwards.
- Zero decks: `targetDeck` stays `null`, Save is disabled and no message
  explains why. Deleting the last deck is refused (`DECK_CURRENT.md`), so this
  state needs a database without any deck. Decks are managed in Settings → Mazos (`DECK_CURRENT.md`).
- Reading the enriched card: `CARD_DETAIL_CURRENT.md`.
- Editing the card after enrichment: `EDIT_FLASHCARD_CURRENT.md`.
- Where captures are listed and searched: `LIBRARY_CURRENT.md`.
- Home CTA that opens this screen: `TODAY_CURRENT.md`.

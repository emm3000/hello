# Current Card Detail

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Card Detail` flow |
| Source of Truth | No |
| Read this when | You need to understand how an existing card is shown and deleted |
| Last verified | 2026-09-09 |

## Summary

`Card Detail` shows a saved flashcard at rest on its own hue and lets you edit or delete it (soft delete), and can play the word and its example sentence aloud. It opens from `Library` (`LibraryRoute` navigates to `CardDetailRoute(cardId, deckId)`). Single vertical scroll, no tabs, no sections.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/card/CardDetailRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/core/ui/TextEmphasis.kt` (`underlineFirstMatch`, used to underline the word inside the example)
- `app/src/main/kotlin/com/emm/hello/core/ui/HSpeakerButton.kt` (shared play/stop button, also used by `Study`)
- `app/src/main/kotlin/com/emm/hello/core/audio/` — `TextToSpeechManager`, `AudioState`, `TtsUtterancePolicy`

## State

`FlashcardDetailUiState` holds:

- `flashcard: Flashcard` (default `Flashcard.empty(SystemClock)`)
- `isLoading: Boolean = true`
- `isDeleteConfirmationVisible: Boolean = false`

## Loading

`FlashcardDetailViewModel` has no `init` block; nothing loads until `FlashcardDetailUiIntent.Load` is sent. `CardDetailDestination` sends `Load` from a `LaunchedEffect(Unit)` every time the destination enters composition.

Because Navigation 3 (1.1.7) renders only the top entry in single-pane and re-composes the previous entry on pop, this means the card is re-fetched when the user returns from Edit Flashcard, so an edited word, meaning, translation, or a status that went from `FAILED` to `ENRICHED`, shows immediately. Rotation also re-fetches once. There is no per-entry `Lifecycle` in Navigation 3, so `LifecycleEventEffect(ON_RESUME)` was deliberately not used (the Activity stays resumed during in-app navigation).

On `Load`, `loadFlashcard()`:

- calls `FlashcardRepository.fetchById(flashcardId)`
- on success sets `flashcard = detail.flashcard` and flips `isLoading = false`
- on error emits `LoadFailed("Couldn't load the card")` (hard-coded literal, not a string resource); `CardDetailDestination` shows it as a `Toast` and navigates back

`isLoading` starts at `true`, so the screen never flashes the empty-flashcard default: while it is `true`, `FlashcardDetailScreen` keeps the top bar and renders `LoadingBody()` (a centered `HLoadingSpinner`) instead of the card body.

## Actions

- `Load` → `loadFlashcard()`
- `BackClicked` (back icon in top bar) → emits `NavigateBack`
- `EditFlashcard` ("Edit" text button in top bar) → emits `NavigateToEditFlashcard(flashcardId)`
- `DeleteFlashcard` ("Delete" destructive item inside the "more" dropdown) → opens confirmation dialog (`isDeleteConfirmationVisible = true`)
- `ConfirmDeleteFlashcard` → closes the dialog, runs `FlashcardRepository.softDeleteFlashcard`, emits `UndoEvent.CardDeleted(flashcardId, deletedAt)` to `UndoEventHolder` so `LibraryViewModel` can show an undo snackbar, then emits `FlashcardDeleted`; on error emits `ShowMessage("Couldn't delete the card")` (hard-coded literal)
- `DismissDeleteFlashcard` → closes dialog

## Effects

`FlashcardDetailUiEffect`:

- `LoadFailed(message)` — toast + `navigator.goBack()`
- `NavigateBack` — `navigator.goBack()`
- `NavigateToEditFlashcard(cardId)` — `navigator.navigateTo(EditFlashcardRoute(cardId, deckId))`
- `FlashcardDeleted` — `navigator.goBack()`
- `ShowMessage(message)` — toast

## TTS

Text-to-speech is wired at `CardDetailDestination`, not through MVI: it never travels through `FlashcardDetailUiState`, `FlashcardDetailUiIntent` or `FlashcardDetailUiEffect`.

`CardDetailDestination` gets `TextToSpeechManager` with `koinInject()`, collects its `speakingUtteranceId` and `isReady` as local `by collectAsStateWithLifecycle()` state, and runs `DisposableEffect(textToSpeech) { textToSpeech.init(); onDispose { textToSpeech.stop() } }` so playback follows the destination's composition, not the ViewModel. It then passes `onSpeak = textToSpeech::speak`, `onStopSpeech = textToSpeech::stop` and `audioState = AudioState(speakingUtteranceId, isTtsReady)` into `FlashcardDetailScreen` as plain parameters alongside `state` and `onIntent`. This is a deliberate deviation from the repo's MVI contract, not an oversight — it is the same shape `StudyRoute` already uses.

`WordBlock` plays `flashcard.word` (utterance id `"card_word"`). `ExampleBlock` plays `example.text` (utterance id `"card_example"`), through a smaller, muted `HSpeakerButton` (`tint = inkSoft`, `iconSize = 20.dp`, `buttonSize = 40.dp`, its own content descriptions `speak_example_desc` / `stop_example_speech_desc`). Both buttons are disabled until `audioState.isTtsReady`; `WordBlock`'s is additionally disabled when `flashcard.word` is blank.

`translation` and `example.translation` get no speaker button: `TextToSpeechManager.init` is pinned to `Locale.US`, so there is no way to speak the Spanish text.

## Layout

Type sizes, families and color tokens are not repeated here; `FlashcardDetailScreen.kt` and `core/theme/` are the only source for them.

The whole screen is a `Surface` colored with `cardHueFor(flashcard.id.value)`. Inside, a `Column` with `safeDrawingPadding()` holds `HTopBar` and then an inner `Column` carrying `MaterialTheme.spacing.screenGutter` horizontal padding and 24.dp bottom padding. The top bar sits outside that gutter so its back arrow aligns with every other screen. The body is a `Column` inside a `verticalScroll`, blocks spaced by 20.dp. No `HSeparator`, `HSectionLabel` or `HCard` is used.

| Block | Content | Notes |
|---|---|---|
| `HTopBar` | Back arrow, then `actions`: `HButton` text variant "Edit" (`R.string.edit`) and `HIconButton` `MoreVert` (`R.string.more_options`) opening an `HDropdownMenu` with one destructive `HMenuItem` "Delete" (`R.string.delete`). | The shared component; the private `DetailTopBar` it replaced is gone. |
| `WordBlock` | A `Row`: a `Column` (`Modifier.weight(1f)`) with the word then `phonetic`, and an `HSpeakerButton` that plays `flashcard.word`. | `phonetic` renders only if non-blank. |
| Translation | `translation` as a large display line. | Rendered only if non-blank; no speaker button (see TTS). |
| `ExampleBlock` | First example (`examples.firstOrNull()`): a `Row` with the example text (`Modifier.weight(1f)`, word underlined via `underlineFirstMatch`) and a smaller, muted `HSpeakerButton` that plays `example.text`; the translation renders below the row. | Skipped if there is no example or its `text` is blank; translation only if non-blank; translation has no speaker button (see TTS). |
| `ReferenceLine` | `partOfSpeech` and `meaning` joined by ` · `. | Blank parts are dropped; line skipped if nothing remains. |
| `CapturedInputLine` | `capturedInput` through `R.string.card_detail_captured_input` ("You typed: %1$s"). | Skipped when `capturedInput` is blank or equals `word` ignoring case. Enrichment overwrites `word` but never `capturedInput`, so this is what the user actually typed. |
| `StatusLine` | `enrichmentStatus`: `PENDING` → "Preparing…" (`R.string.library_status_pending`), `FAILED` → "Failed" (`R.string.library_status_failed`, destructive ink). | `ENRICHED` renders nothing. |

The delete confirmation is an `HAlertDialog` with `isDangerous = true`, title `R.string.delete_flashcard_title` ("Delete card"), description `R.string.delete_flashcard_description`, confirm `R.string.delete`, cancel `R.string.cancel`.

## Persistence

- Read: `FlashcardRepository.fetchById` (local).
- Delete: soft delete via `FlashcardRepository.softDeleteFlashcard`.
- No remote sync involved.

## Strings

Keys referenced by `FlashcardDetailScreen.kt` (`app/src/main/res/values/strings.xml`, English copy):

- `edit`, `delete`, `cancel`, `more_options`
- `delete_flashcard_title`, `delete_flashcard_description`
- `library_status_pending`, `library_status_failed`
- `speak_desc`, `stop_speech_desc` (`WordBlock`'s `HSpeakerButton`)
- `speak_example_desc`, `stop_example_speech_desc` (`ExampleBlock`'s `HSpeakerButton`)

The load and delete error messages emitted by `FlashcardDetailViewModel` are hard-coded English literals, not resources.

The former `card_detail_*` keys and `confusable_with_label` no longer exist in `strings.xml`; the dictionary-entry layout (`HDictSense`, senses, examples list, extras, context, footer) was removed with the redesign.

# Current Suggest

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Suggest` flow (new words for a situation) |
| Source of Truth | No |
| Read this when | You need to understand how AI-suggested words are generated, picked and captured |
| Last verified | 2026-09-05 |

## Summary

`Suggest` proposes six English words or expressions for one everyday situation,
seeded from the user's own recent vocabulary, and lets the user pick which
ones to capture as new flashcards. It is reached from two places, never from
its own tab: `Hoy` when nothing is due, and the end of a study session.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/suggest/SuggestRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/suggest/SuggestScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/suggest/SuggestViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/suggest/SuggestUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/suggest/SuggestUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/suggest/SuggestUiEffect.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/SuggestWordsUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/SuggestedWord.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/WordSuggestions.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/WordSuggestionRepository.kt`
- `domain/src/main/kotlin/com/emm/domain/authoring/CaptureFlashcardUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/connectivity/ConnectivityRepository.kt`
- `data/src/main/kotlin/com/emm/data/connectivity/AndroidConnectivityRepository.kt`
- `data/src/main/kotlin/com/emm/data/suggestion/GeminiWordSuggestionRepository.kt`
- `data/src/main/kotlin/com/emm/data/suggestion/CannedWordSuggestionRepository.kt`
- `data/src/main/kotlin/com/emm/data/suggestion/WordSuggestionPrompt.kt`
- `data/src/main/kotlin/com/emm/data/suggestion/WordSuggestionResponse.kt`
- `app/src/main/kotlin/com/emm/hello/enrichment/FlashcardEnrichmentScheduler.kt`
- `app/src/main/kotlin/com/emm/hello/di/NewModule.kt`

## Entry points

- **`Hoy`, zero cards due** — `TodayScreen` renders the `else` branch of its
  hero `when` (the counterpart to `state.hasSessionReady`) and shows a
  `today_get_new_words` button. `TodayRoute` wires `onGetNewWords` to
  `navigator.navigateTo(SuggestRoute)` directly; there is no intent or effect
  hop through `TodayViewModel`.
- **End of a study session** — `StudyScreen`'s `StudyStage.Done` stage shows a
  `study_done_get_new_words` button. It dispatches
  `StudyUiIntent.GetNewWordsClicked`, `StudyViewModel` turns that into
  `StudyUiEffect.NavigateToSuggest`, and `StudyRoute` collects the effect and
  calls `navigator.navigateTo(SuggestRoute)`.

Both entry points land on the same `SuggestRoute` destination.

## State

`SuggestUiState`:

- `isLoading: Boolean` — true until the first `SuggestWordsUseCase` result (or
  failure) arrives; defaults `true`.
- `loadFailed: Boolean` — true when the load threw.
- `isOffline: Boolean` — true when `load()` finds the device offline before
  ever calling `SuggestWordsUseCase`; defaults `false`.
- `situation: String` — the one-sentence English situation the words belong
  to (e.g. "Ordering food at a busy restaurant").
- `words: List<SuggestedWord>` — the candidate words for this situation, each
  an English `word` plus its Spanish `translation`.
- `selectedWords: Set<String>` — the words (by their `word` string) the user
  has toggled on.
- `isAdding: Boolean` — true while `AddSelected` is in flight.

Two values are derived, not stored:

- `selectedCount: Int` — `selectedWords.size`.
- `canAdd: Boolean` — `selectedCount > 0 && !isAdding`.

## Intents

`SuggestUiIntent`:

- `Retry` — re-runs the load (used from the error and empty states).
- `WordToggled(word)` — adds or removes `word` from `selectedWords`.
- `AddSelected` — captures every selected word as a new flashcard; no-ops if
  `canAdd` is false.
- `BackClicked` — leaves the screen without adding anything.

## Effects

`SuggestUiEffect`:

- `EnqueueEnrichment(flashcardIds: List<String>)` — `SuggestRoute` converts
  each raw id with `toFlashcardId()` and calls
  `FlashcardEnrichmentScheduler.enqueue(context, flashcardId)` for it.
- `ShowMessage(@StringRes messageRes)` — `SuggestRoute` shows it as a
  `Toast.makeText(..., Toast.LENGTH_SHORT)`.
- `NavigateBack` — `SuggestRoute` calls `navigator.goBack()`.

## Screen

Full-bleed `cardPeriwinkle` background behind an `HTopBar` with only a back
action. Body state is a `when` over `isLoading` / `isOffline` / `loadFailed` /
`words.isEmpty()`, checked in that order:

- **Loading** — centered `HLoadingSpinner` plus `suggest_loading`.
- **Offline** (`isOffline`) — `HEmptyState` with `suggest_offline_title`
  ("You're offline") / `suggest_offline_body`, a primary `suggest_retry` CTA
  (`Retry`) and a ghost `suggest_not_now` CTA (`BackClicked`); same layout as
  the error state.
- **Error** (`loadFailed`) — `HEmptyState` with `suggest_error_title` /
  `suggest_error_body`, a primary `suggest_retry` CTA (`Retry`) and a ghost
  `suggest_not_now` CTA (`BackClicked`).
- **Empty** (load succeeded but every candidate got filtered out) —
  `HEmptyState` with `suggest_empty_title` / `suggest_empty_body` and only a
  primary `suggest_retry` CTA.
- **Loaded** — an uppercase `suggest_eyebrow` ("NEW WORDS") label, the
  `situation` as a Bricolage headline, then an `FlowRow` of `HChip` pills, one
  per word, labelled `"${word.word} · ${word.translation}"` and toggled
  active by membership in `selectedWords`.

The dock below the chips is always the same two buttons: a primary `HButton`
whose label is `suggest_pick_some` ("Pick the words you want") when nothing is
selected or the plural `suggest_add_selected` ("Add N word(s)") once
`selectedCount > 0`, disabled unless `canAdd`; and a ghost `suggest_not_now`
button that sends `BackClicked`.

## Data path

`SuggestViewModel.load()` first resets `isOffline` to `false`, then checks
`connectivityRepository.observeOnline().first()`. If offline, it sets
`isLoading = false, isOffline = true` and returns without calling
`SuggestWordsUseCase` at all — `Retry` re-runs `load()`, so it re-checks.
Otherwise it proceeds to:

`SuggestWordsUseCase(flashcardRepository, suggestionRepository)`:

1. Calls `flashcardRepository.fetchRecentWords(20)` to seed the AI with the
   learner's last 20 captured words.
2. Calls `suggestionRepository.suggest(recentWords)` to get one
   `WordSuggestions` (a situation plus candidate words).
3. Filters the candidates: drops any whose `word` or `translation` is blank,
   any whose normalized (`trim().lowercase()`) `word` already appears in
   `recentWords`, and any duplicate normalized `word` within the candidate
   list itself.

`WordSuggestionRepository` has two implementations, chosen in
`app/src/main/kotlin/com/emm/hello/di/NewModule.kt` by `BuildConfig.USE_CANNED_AI`:

- **`GeminiWordSuggestionRepository`** (used when `USE_CANNED_AI` is false) —
  builds a strict-JSON prompt with `WordSuggestionPrompt.build(recentWords)`,
  makes exactly one `geminiService.process(prompt)` call (one quota consume),
  and parses the result with `WordSuggestionResponseParser`, which strips
  Markdown code fences and decodes `{"situation": ..., "words": [...]}`,
  throwing if `situation` is blank or `words` is empty.
- **`CannedWordSuggestionRepository`** (used when `USE_CANNED_AI` is true) —
  no network call. Waits 600 ms, then returns one of three fixed scenarios
  (restaurant ordering, asking for directions, a junior job interview), each
  with six hardcoded words, picked by `recentWords.size % 3`.

`app/build.gradle.kts` sets `USE_CANNED_AI` to `true` for `debug`, `false` for
`release` and `false` for `staging`. Debug builds use the canned repository
because Firebase AI Logic is unavailable until App Check is enforced.

Picking words and confirming (`AddSelected`) resolves a target deck via
`GetDecksUseCase` and `DefaultDeckSelectionRepository` (the default deck if
set, else the first deck in the list); if there is no deck at all it shows
`suggest_error_no_deck` and stops. For each selected word,
`CaptureFlashcardUseCase(deckId, word, translation)` creates a `PENDING`
flashcard in that deck carrying the suggestion's Spanish translation as its
`translation` field (`meaning` and `phonetic` stay empty, to be filled by
enrichment). A word that is already a duplicate in the target deck
(`DomainValidationException` with `IssueCode.DuplicateWordInDeck`) is skipped
silently rather than failing the whole batch; any other capture failure
propagates and shows `suggest_error_add`. Every flashcard id that was
successfully created is enqueued for background enrichment via
`FlashcardEnrichmentScheduler`, then `suggest_added` is shown and the screen
navigates back.

## Not in scope

- No situation picker or history — one situation is offered per load, and
  `Retry` discards it for a new one.
- No per-word edit before capture; the translation is whatever the
  suggestion repository returned.
- No manual situation prompt from the user; the situation is chosen entirely
  by the suggestion repository.
- No offline queue for the Gemini path — once online, a failed
  `geminiService.process` call surfaces as the error state and does not retry
  automatically. Being offline is caught earlier: `load()` checks
  `ConnectivityRepository.observeOnline()` before ever calling
  `SuggestWordsUseCase`, so the offline state is shown instead of attempting
  the network call.

## Related docs

- `docs/TODAY_CURRENT.md` — the `Hoy` entry point.
- `docs/DESIGN_BRIEF.md` — visual direction (`cardPeriwinkle`, chip and dock
  usage).

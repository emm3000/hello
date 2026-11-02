# Current Suggest

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Suggest` flow (new words for a situation) |
| Source of Truth | No |
| Read this when | You need to understand how AI-suggested words are generated, picked and captured |
| Last verified | 2026-11-02 |

## Summary

`Suggest` proposes 3 single English words and 3 full natural phrases for one
everyday situation, seeded from the user's own recent vocabulary, and lets the
user pick which ones to capture as new flashcards. It never repeats a
situation or item shown in the last 60 days. It is reached from two places,
never from its own tab: `Hoy` when nothing is due, and the end of a study
session.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/suggest/SuggestRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/suggest/SuggestScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/suggest/SuggestViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/suggest/SuggestUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/suggest/SuggestUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/suggest/SuggestUiEffect.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/SuggestedWordsRefresher.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/ObserveSuggestedWordsUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/RefreshSuggestedWordsUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/MarkSuggestedWordsSeenUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/PrepareDailyWordsUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/SuggestedWordFilter.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/SuggestedWord.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/WordSuggestions.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/WordSuggestionRequest.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/WordSuggestionRepository.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/WordSuggestionCache.kt`
- `domain/src/main/kotlin/com/emm/domain/suggestion/SuggestionHistoryRepository.kt`
- `data/src/main/kotlin/com/emm/data/suggestion/DefaultWordSuggestionCache.kt`
- `data/src/main/kotlin/com/emm/data/suggestion/DefaultSuggestionHistoryRepository.kt`
- `data/src/main/sqldelight/com/emm/data/Suggestion.sq`
- `domain/src/main/kotlin/com/emm/domain/authoring/CaptureFlashcardUseCase.kt`
- `domain/src/main/kotlin/com/emm/domain/connectivity/ConnectivityRepository.kt`
- `data/src/main/kotlin/com/emm/data/connectivity/AndroidConnectivityRepository.kt`
- `data/src/main/kotlin/com/emm/data/suggestion/RemoteWordSuggestionRepository.kt`
- `data/src/main/kotlin/com/emm/data/suggestion/CannedWordSuggestionRepository.kt`
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

- **Daily words notification** — `DailyWordsWorker` posts the batch
  `PrepareDailyWordsUseCase` prepared; tapping it opens `MainActivity` with
  `LaunchDestination.DailyWords`, which `NewRoot` resets to
  `TodayRoute` → `SuggestRoute`. See `docs/NOTIFICATIONS_PLAN.md`.

All three entry points land on the same `SuggestRoute` destination.

## State

`SuggestUiState`:

- `isLoading: Boolean` — true while there are no usable words and the
  refresher status is `Idle` or `Running`; defaults `true`.
- `loadFailed: Boolean` — true when there are no usable words and the last
  refresh failed.
- `isOffline: Boolean` — true when there are no usable words and the last
  refresh found the device offline; defaults `false`.
- `situation: String` — the one-sentence English situation the words belong
  to (e.g. "Ordering food at a busy restaurant").
- `words: List<SuggestedWord>` — the candidate words for this situation, each
  an English `word` plus its Spanish `translation`.
- `selectedWords: Set<String>` — the words (by their `word` string) the user
  has toggled on.
- `isAdding: Boolean` — true while `AddSelected` is in flight.
- `creditsRemaining: Int?` — today's remaining AI generations from
  `GenerationCreditsRepository`, or `null` when no fresh reading exists.

Four values are derived, not stored:

- `selectedCount: Int` — `selectedWords.size`.
- `canAdd: Boolean` — `selectedCount > 0 && !isAdding`.
- `isCreditsNoticeVisible: Boolean` — `creditsRemaining` is known and not
  above the number of words.
- `canSelectMore: Boolean` — `creditsRemaining` is unknown or above
  `selectedCount`.

## Intents

`SuggestUiIntent`:

- `Retry` — forces a new batch through `SuggestedWordsRefresher.refresh()`
  (used from the offline, error and empty states).
- `WordToggled(word)` — adds or removes `word` from `selectedWords`. Adding
  past `creditsRemaining` is refused with a `suggest_daily_cap_*` message.
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
  `situation` as a Bricolage headline, the plural `suggest_credits_remaining`
  notice when `isCreditsNoticeVisible`, then an `FlowRow` of `HChip` pills, one
  per word, labelled `"${word.word} · ${word.translation}"` and toggled
  active by membership in `selectedWords`.

The dock below the chips is always the same two buttons: a primary `HButton`
whose label is `suggest_pick_some` ("Pick the words you want") when nothing is
selected or the plural `suggest_add_selected` ("Add N word(s)") once
`selectedCount > 0`, disabled unless `canAdd`; and a ghost `suggest_not_now`
button that sends `BackClicked`.

## Data path

The screen reads one cached batch and never fetches on its own.
`SuggestViewModel` combines `ObserveSuggestedWordsUseCase()`,
`SuggestedWordsRefresher.status` and the credits reading, and renders from
them. On creation it calls `refresher.ensure()`; `Retry` calls
`refresher.refresh()`.

**The cache.** `DefaultWordSuggestionCache` keeps a singleton
`WordSuggestionBatch` row (situation, `createdAt`, nullable `firstSeenAt`) and
its `SuggestedWordCandidate` rows in `HelloDb`, so re-entering `Suggest` does
not call the paid backend. `ObserveSuggestedWordsUseCase` maps the cached
batch through `SuggestedWordFilter.usable(words)`, which drops any candidate
that is now a card.

**The refresher.** `SuggestedWordsRefresher` is a singleton that serializes
requests on its own scope. `ensure()` does nothing when the cached batch still
has usable words; otherwise, like `refresh()`, it checks
`ConnectivityRepository.observeOnline()`. Offline sets status `Offline`
without a call. Online sets `Running`, runs `RefreshSuggestedWordsUseCase`
and ends `Completed`, or `Failed(error)` on a throw. Requests queued while a
fetch attempt runs are discarded once it ends.

**`RefreshSuggestedWordsUseCase`:**

1. Deletes `SuggestionHistory` entries shown more than 60 days ago.
2. Reads the remaining history, most recent first.
3. Calls `suggestionRepository.suggest(WordSuggestionRequest)` with the
   learner's last 20 card words (`fetchRecentWords(20)`), the 30 most recent
   distinct history situations and the 120 most recent distinct history items.
4. Filters the candidates with `SuggestedWordFilter.usable(words, history)`.
   It drops any candidate whose `word` or `translation` is blank, whose
   `searchNormalized()` `word` matches any non-deleted card (all of them, not
   only the last 20) or any history item, or that repeats an earlier candidate.
5. Replaces the cached batch with the result and returns it. The new batch
   starts unseen. In the same transaction the cache writes its situation and
   every stored item to `SuggestionHistory`, all stamped with the batch's
   `createdAt`. History is written when a batch is stored, not when it is
   seen.

The cached batch's own items are already in the history, so the history
filter runs only on a fresh response, never in `ObserveSuggestedWordsUseCase`.

**Seen marker.** When `SuggestViewModel` renders a batch with usable words
whose `firstSeenAt` is null, it calls `MarkSuggestedWordsSeenUseCase`. The
cache sets `firstSeenAt` to the clock's time only while it is still null, so
the first value is kept. The loading, offline, error and empty states never
mark it.

**`PrepareDailyWordsUseCase`** prepares the batch for the daily words
notification without spending a call on a batch nobody opened. It returns the
cached batch when it is unseen and still has usable words. Otherwise it
returns null when offline, and when online it runs
`RefreshSuggestedWordsUseCase` and returns the new batch. It returns null if
that throws, which leaves the cached batch unchanged, or if the new batch has
no usable words.

`WordSuggestionRepository` has two implementations, chosen in
`app/src/main/kotlin/com/emm/hello/di/NewModule.kt` by `BuildConfig.USE_CANNED_AI`:

- **`RemoteWordSuggestionRepository`** (used when `USE_CANNED_AI` is false) —
  calls the Supabase edge function `suggest-words` through `FunctionsTransport`,
  after `SessionInitializer.ensureSession()` and attaching an
  `AppCheckTokenProvider.token()`, sending `SuggestWordsRequestDto` with
  `recent_words`, `excluded_situations` and `excluded_words`.
  The reply is decoded with `FunctionsReplyMapper.map` plus
  `WordSuggestionResponseParser`, which strips Markdown code fences and decodes
  `{"situation": ..., "words": [...]}`, throwing if `situation` is blank or
  `words` is empty. A call or parse failure is recorded on `GenerationTelemetry`
  and rethrown; the repository itself does not retry.
- **`CannedWordSuggestionRepository`** (used when `USE_CANNED_AI` is true) —
  no network call. Waits 600 ms, then returns one of three fixed scenarios
  (restaurant ordering, asking for directions, a junior job interview), each
  with 3 single words and 3 phrases. It returns the first scenario whose
  situation is not in `excludedSituations`. When all three are excluded, it
  returns a random scenario with a random 8-character suffix on the situation
  and on every item, so the history filter keeps it and debug builds keep
  producing batches.

`app/build.gradle.kts` sets `USE_CANNED_AI` to `true` for `debug` and `false`
for `release` (only these two build types exist). Debug builds keep the canned
repository so Suggest stays deterministic and offline while developing. Both
word suggestions and Capture's card enrichment go through Supabase edge
functions (`suggest-words` and `generate-note` respectively) over the same
`functions-kt` transport, authenticated with a Firebase App Check token — see
`docs/AI_BACKEND_PLAN.md`.

On the backend, `buildWordSuggestionPrompt` in
`supabase/functions/_shared/prompt.ts` asks for exactly 3 single words and
exactly 3 full natural phrases a native speaker would say in the situation,
each with a Spanish translation. `suggestWordsRequestSchema` also accepts two
optional exclusion lists, `excluded_situations` (at most 30 strings of 1 to 200
chars) and `excluded_words` (at most 120 strings of 1 to 120 chars), both
defaulting to `[]`. Non-empty lists are added to the prompt as situations and
items the model must not reuse; an empty list adds no text. A list over its cap
or an empty string is rejected as `invalid_request` before a credit is taken.
The response shape is unchanged, and phrases come back in the same `word` field.

Picking words and confirming (`AddSelected`) resolves a target deck via
`GetDecksUseCase` and `ResolveCaptureDeckUseCase`, the same rule Capture uses
(the stored default deck if it is one of the user's own decks, else the oldest
of those, with Store installs counted only when no other deck exists); if
there is no deck at all it shows
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
navigates back. If every selected word turns out to be a duplicate, nothing
was created: `suggest_all_known` is shown instead, `selectedWords` is cleared,
`isAdding` goes back to `false`, and the screen stays put so the user can pick
other words or tap `suggest_not_now`.

## Not in scope

- No situation picker — one situation is offered per batch, and `Retry`
  discards it for a new one. The history only feeds exclusions; the learner
  never sees it.
- No per-word edit before capture; the translation is whatever the
  suggestion repository returned.
- No manual situation prompt from the user; the situation is chosen entirely
  by the suggestion repository.
- No offline queue for the remote path — once online, a failed `suggest-words`
  call surfaces as the error state and does not retry automatically. Being
  offline is caught earlier: the refresher checks
  `ConnectivityRepository.observeOnline()` before calling
  `RefreshSuggestedWordsUseCase`, so the offline state is shown instead of
  attempting the network call.

## Related docs

- `docs/TODAY_CURRENT.md` — the `Hoy` entry point.
- `docs/DESIGN_BRIEF.md` — visual direction (`cardPeriwinkle`, chip and dock
  usage).

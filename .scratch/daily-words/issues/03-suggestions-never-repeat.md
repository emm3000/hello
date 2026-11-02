# 03: Suggest: never repeat a shown situation or item, and record when a batch was seen

Status: done
Device: none
Blocked by: 01
**Branch:** feat/03-suggestions-never-repeat

Spec decisions 3, 4 and 6. Gate row 3 (Koin `app/src/main/kotlin/com/emm/hello/di/NewModule.kt`). Load `mattpocock-skills:tdd`. `docs/SUGGEST_CURRENT.md` has drifted (it names `SuggestWordsUseCase`, which no longer exists, while the real flow is `SuggestedWordsRefresher` → `RefreshSuggestedWordsUseCase` → `WordSuggestionCache`); fix the doc in this change.
Code: `domain/src/main/kotlin/com/emm/domain/suggestion/` (`RefreshSuggestedWordsUseCase`, `SuggestedWordFilter`, `WordSuggestionRepository`, `WordSuggestionCache`), `data/src/main/kotlin/com/emm/data/suggestion/` (`DefaultWordSuggestionCache`, `RemoteWordSuggestionRepository`, `CannedWordSuggestionRepository`), `data/src/main/kotlin/com/emm/data/remote/SuggestWordsRequestDto.kt`, the `Suggestion.sq` queries over ticket 01's schema, and `newfeatures/suggest/SuggestViewModel`.
The wire contract comes from 02: `excluded_situations` holds at most 30 entries and `excluded_words` at most 120, most recent first. The deployed `suggestWordsRequestSchema` is a plain `z.object`, which strips unknown keys, so this ticket does not wait for 02's deploy.

## Acceptance
- [x] `DefaultWordSuggestionCacheTest`: storing a batch writes its situation and every stored item to `SuggestionHistory` with the clock's time, in the same transaction as the batch
- [x] Domain test: a refresh deletes history entries older than 60 days and keeps newer ones
- [x] Domain test: a refresh passes the most recent history to the repository as exclusions, capped at 30 situations and 120 items
- [x] `SuggestedWordFilterTest`: an item that equals a history entry after `searchNormalized` is dropped. An item that already exists as a card is dropped even when that card is older than the last `RECENT_WORDS_LIMIT`
- [x] New `PrepareDailyWordsUseCaseTest` in `:domain:test`. An unseen batch with usable items is returned and the repository is not called. With a seen, absent, or unseen-but-exhausted (no usable items) batch, offline returns null with no call, and online fetches and returns the new batch. A thrown fetch returns null and leaves the cached batch unchanged. A fetched batch with no usable items returns null
- [x] `SuggestViewModelTest`: rendering a loaded batch sets its first-seen time once, and a later render keeps the first value. The loading, offline, error and empty states never set it
- [x] `RemoteWordSuggestionRepositoryTest`: the request body carries `excluded_situations` and `excluded_words`
- [x] Each `CannedWordSuggestionRepository` scenario has 3 single words and 3 phrases, and a scenario whose situation is excluded is skipped, so debug builds keep producing batches
- [x] `scripts/check` green

## Comments

- Done in 4c8c5584 (code and tests) and a3c2b770 (`docs/SUGGEST_CURRENT.md`, rebased on ticket 02's version of it, Summary now states the 3 words and 3 phrases mix).
- Every acceptance test exists and passes: `DefaultWordSuggestionCacheTest` (history written with the clock's time; a trigger that rejects one history insert rolls back the batch too; `markSeen` keeps the first value), `RefreshSuggestedWordsUseCaseTest` (entries 61 days old deleted, 59 days kept; 35 situations and 125 items capped to the most recent 30 and 120, de-duplicated; a history item dropped), `SuggestedWordFilterTest` (`Résumé` in history drops `resume`; a card older than the last 20 drops its candidate), `PrepareDailyWordsUseCaseTest` (9 cases), `SuggestViewModelTest` (first-seen set once; loading, offline, error, empty never set it), `RemoteWordSuggestionRepositoryTest` (body carries `excluded_situations` and `excluded_words`), `CannedWordSuggestionRepositoryTest` (3 distinct scenarios, each 3 single words and 3 phrases; an excluded situation is skipped). Added `DefaultSuggestionHistoryRepositoryTest` (2 cases).
- Red first: each new test failed on an assertion against the old behaviour before its implementation. The offline and thrown-fetch cases of `PrepareDailyWordsUseCaseTest` passed against a null stub, so they were checked by removing the offline guard and narrowing the catch: all four failed, then passed once restored. `SuggestViewModelTest`'s "once" case fails with the `firstSeenAt == null` guard removed.
- The history filter runs only on a fresh response. Applying it in `ObserveSuggestedWordsUseCase` would empty every cached batch, because its own items are already in the history.
- `FlashcardRepository.fetchAllWords()` is new; 11 test fakes got its one-line override.
- `scripts/check` green in the pre-commit hook on both commits (detekt, testDebugUnitTest, :domain:test, :data:verifySqlDelightMigration). No device: the ticket is `Device: none` and no screen layout changed.
- Review fix: once every canned situation is excluded, `CannedWordSuggestionRepository` returns a scenario with a random UUID suffix on its situation and items, so debug builds keep producing batches past the third. `CannedWordSuggestionRepositoryTest` puts every canned item in history and checks that two consecutive variants each keep all 6 items through `SuggestedWordFilter`. That test failed against the old fallback, which returned the least recently shown scenario. `SuggestViewModel.markSeen()` now declares its `Job` return type.

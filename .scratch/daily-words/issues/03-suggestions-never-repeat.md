# 03: Suggest: never repeat a shown situation or item, and record when a batch was seen

Status: ready-for-agent
Device: none
Blocked by: 01
**Branch:** feat/03-suggestions-never-repeat

Spec decisions 3, 4 and 6. Gate row 3 (Koin `app/src/main/kotlin/com/emm/hello/di/NewModule.kt`). Load `mattpocock-skills:tdd`. `docs/SUGGEST_CURRENT.md` has drifted (it names `SuggestWordsUseCase`, which no longer exists, while the real flow is `SuggestedWordsRefresher` → `RefreshSuggestedWordsUseCase` → `WordSuggestionCache`); fix the doc in this change.
Code: `domain/src/main/kotlin/com/emm/domain/suggestion/` (`RefreshSuggestedWordsUseCase`, `SuggestedWordFilter`, `WordSuggestionRepository`, `WordSuggestionCache`), `data/src/main/kotlin/com/emm/data/suggestion/` (`DefaultWordSuggestionCache`, `RemoteWordSuggestionRepository`, `CannedWordSuggestionRepository`), `data/src/main/kotlin/com/emm/data/remote/SuggestWordsRequestDto.kt`, the `Suggestion.sq` queries over ticket 01's schema, and `newfeatures/suggest/SuggestViewModel`.
The wire contract comes from 02: `excluded_situations` holds at most 30 entries and `excluded_words` at most 120, most recent first. The deployed `suggestWordsRequestSchema` is a plain `z.object`, which strips unknown keys, so this ticket does not wait for 02's deploy.

## Acceptance
- [ ] `DefaultWordSuggestionCacheTest`: storing a batch writes its situation and every stored item to `SuggestionHistory` with the clock's time, in the same transaction as the batch
- [ ] Domain test: a refresh deletes history entries older than 60 days and keeps newer ones
- [ ] Domain test: a refresh passes the most recent history to the repository as exclusions, capped at 30 situations and 120 items
- [ ] `SuggestedWordFilterTest`: an item that equals a history entry after `searchNormalized` is dropped. An item that already exists as a card is dropped even when that card is older than the last `RECENT_WORDS_LIMIT`
- [ ] New `PrepareDailyWordsUseCaseTest` in `:domain:test`. An unseen batch with usable items is returned and the repository is not called. With a seen, absent, or unseen-but-exhausted (no usable items) batch, offline returns null with no call, and online fetches and returns the new batch. A thrown fetch returns null and leaves the cached batch unchanged. A fetched batch with no usable items returns null
- [ ] `SuggestViewModelTest`: rendering a loaded batch sets its first-seen time once, and a later render keeps the first value. The loading, offline, error and empty states never set it
- [ ] `RemoteWordSuggestionRepositoryTest`: the request body carries `excluded_situations` and `excluded_words`
- [ ] Each `CannedWordSuggestionRepository` scenario has 3 single words and 3 phrases, and a scenario whose situation is excluded is skipped, so debug builds keep producing batches
- [ ] `scripts/check` green
